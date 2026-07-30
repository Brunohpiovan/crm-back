package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Mensagem;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.*;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import br.edu.faculdadevincit.crm_vincit.repository.MensagemRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ParticipanteRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ProtocoloRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProtocoloService {

    @Autowired
    private ProtocoloRepository protocoloRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private MensagemRepository mensagemRepository;

    public ProtocoloMoveDTO createProtocolo(ProtocoloDto protocoloDto) {
        Usuario admin = usuarioRepository.findById(protocoloDto.getId_admin()).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));
        Optional<Protocolo> protocoloOptional = protocoloRepository.findByParticipanteIdAndStatusAberto(protocoloDto.getId_participante(),StatusProtocolo.ABERTO);
        if(protocoloOptional.isPresent()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe um protocolo em andamento com esse usuário");
        }
        Participante participante = participanteRepository.findById(protocoloDto.getId_participante()).orElseGet(()->createParticipante(protocoloDto.getId_participante()));
        Protocolo protocolo = new Protocolo();
        protocolo.setAdmin(admin);
        protocolo.setParticipante(participante);
        protocolo.setStatus(StatusProtocolo.ABERTO);
        protocolo.setDataCriacao(LocalDateTime.now());
        protocoloRepository.save(protocolo);
        List<Mensagem> mensagensSemProtocolo = mensagemRepository.findBySenderIdAndProtocoloIsNull(participante.getId());

        for (Mensagem mensagem : mensagensSemProtocolo) {
            mensagem.setProtocolo(protocolo);
        }

        mensagemRepository.saveAll(mensagensSemProtocolo);
        ProtocoloMoveDTO dto = new ProtocoloMoveDTO(protocolo);
        messagingTemplate.convertAndSend("/topic/protocolo/aberto/" + protocolo.getParticipante().getId(), dto);
        ProtocoloNotificacaoDTO notificacaoDTO = new ProtocoloNotificacaoDTO(participante.getId(), admin.getId());
        messagingTemplate.convertAndSend("/topic/protocolo/novo", notificacaoDTO);
        return dto;
    }

    public Participante createParticipante(Long id){
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(()-> new RuntimeException("Nao existe um usuario com esse Id"));
        Optional<Participante> participante = participanteRepository.findByLogin(usuario.getLogin());
        if(participante.isPresent()){
            return participante.get();
        }else{
            Participante newParticipante = new Participante();
            newParticipante.setNome(usuario.getNome());
            newParticipante.setUrlPicture(usuario.getUrlPicture());
            newParticipante.setLogin(usuario.getLogin());
            newParticipante.setRg(usuario.getRg());
            newParticipante.setCpf(usuario.getCpf());
            newParticipante.setDataNascimento(usuario.getDataNascimento());
            newParticipante.setCelular(usuario.getCelular());
            newParticipante.setEndereco(usuario.getEndereco());
            newParticipante.setNumeroResidencial(usuario.getNumeroResidencial());
            newParticipante.setComplemento(usuario.getComplemento());
            newParticipante.setBairro(usuario.getBairro());
            newParticipante.setUf(usuario.getUf());
            newParticipante.setCidade(usuario.getCidade());
            newParticipante.setObservacoes(usuario.getObservacoes());
            return participanteRepository.save(newParticipante);
        }


    }

    public void closeProtocolo(Long protocolId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailAdmin = authentication.getName();
        Usuario admin = (Usuario) usuarioRepository.findByLogin(emailAdmin)
                .orElseThrow(() -> new RuntimeException("Admin não encontrado"));
        Protocolo protocolo = protocoloRepository.findById(protocolId).orElseThrow(()-> new RuntimeException("Protocolo nao encontrado"));
        if (StatusProtocolo.FECHADO.equals(protocolo.getStatus())) {
            throw new RuntimeException("Protocolo ja encerrado");
        }
        protocolo.setStatus(StatusProtocolo.FECHADO);
        messagingTemplate.convertAndSend("/topic/protocolo/"+protocolo.getId(), protocolo.getStatus());
        protocolo.setDataEncerramento(LocalDateTime.now());
        protocoloRepository.save(protocolo);
        ParticipanteDTO dto = new ParticipanteDTO(protocolo.getParticipante());
        messagingTemplate.convertAndSend("/topic/contatoRet", dto);
    }

    public List<Protocolo> getProtocolsForAdmin(Usuario admin) {
        return protocoloRepository.findByAdmin(admin);
    }

    public List<Protocolo> getProtocolsForUser (Participante participante) {
        return protocoloRepository.findByParticipante(participante);
    }

    public Optional<Protocolo> getProtocoloByUsuario(Long id_usuario1, Long id_usuario2) {
        Usuario usuario1 = usuarioRepository.findById(id_usuario1)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário 1 não encontrado"));

        Participante participante = participanteRepository.findById(id_usuario2)
                .orElseThrow(() -> new UsernameNotFoundException("Participante não encontrado"));

        String celular1 = usuario1.getCelular();
        String celular2 = participante.getCelular();

        return protocoloRepository.findByAdminCelularAndParticipanteCelularAndStatus(celular1, celular2, StatusProtocolo.ABERTO);
    }


    public List<ProtocoloMoveDTO> getProtocols(Long id_usuario) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        Usuario usuario = usuarioRepository.findById(id_usuario).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));
        if (!usuario.getLogin().equals(authenticatedUsername)) {
            throw new br.edu.faculdadevincit.crm_vincit.service.exceptions.AccessDeniedException("Você não tem permissão para acessar este usuário.");
        }
        String login = usuario.getLogin();
        Optional<List<Protocolo>> optionalProtocolos  = protocoloRepository.findByAdminLoginOrParticipanteLogin(login);
        if (optionalProtocolos.isPresent() && !optionalProtocolos.get().isEmpty()) {
            return optionalProtocolos.get().stream().map(ProtocoloMoveDTO::new).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    public ProtocoloMoveDTO findById(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        Optional<Protocolo> protocolo = protocoloRepository.findById(id);
        if (protocolo.isPresent()) {
            Protocolo p = protocolo.get();
            if (!p.getAdmin().getLogin().equals(authenticatedUsername) && !p.getParticipante().getLogin().equals(authenticatedUsername)) {
                throw new RuntimeException("Usuário não autorizado a acessar este protocolo.");
            }
            return new ProtocoloMoveDTO(p);
        } else {
            throw new RuntimeException("No content");
        }
    }

    public ProtocoloMoveDTO encaminha(Long id_admin,Long id_Protocolo){
        Protocolo protocolo = protocoloRepository.findById(id_Protocolo).orElseThrow(()->new RuntimeException("Protocolo nao encontrado"));
        Usuario usuario_adm = usuarioRepository.findById(id_admin)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        Optional<Protocolo> opcional = getProtocoloByUsuario(id_admin,protocolo.getParticipante().getId());
        if (opcional.isPresent()) {
            throw new Error("Já existe um protocolo aberto para este usuário.");
        }
        protocolo.setAdminAnterior(protocolo.getAdmin());
        protocolo.setAdmin(usuario_adm);
        ProtocoloMoveDTO dto = new ProtocoloMoveDTO(protocolo);
        ParticipanteDTO participanteDTO = new ParticipanteDTO(protocolo.getParticipante());
        ProtocoloNotificacao2DTO notify = new ProtocoloNotificacao2DTO(dto,participanteDTO);
        messagingTemplate.convertAndSend("/topic/protocolo/aberto/" + usuario_adm.getId(), notify);
        return new ProtocoloMoveDTO(protocoloRepository.save(protocolo));
    }


}

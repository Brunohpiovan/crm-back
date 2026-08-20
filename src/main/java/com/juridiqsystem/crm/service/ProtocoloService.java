package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.Mensagem;
import com.juridiqsystem.crm.model.Participante;
import com.juridiqsystem.crm.model.Protocolo;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.*;
import com.juridiqsystem.crm.model.enums.StatusProtocolo;
import com.juridiqsystem.crm.repository.MensagemRepository;
import com.juridiqsystem.crm.repository.ParticipanteRepository;
import com.juridiqsystem.crm.repository.ProtocoloRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

    @Transactional
    public ProtocoloMoveDTO createProtocolo(ProtocoloDto protocoloDto) {
        Usuario admin = usuarioRepository.findByPublicId(protocoloDto.getId_admin()).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));
        Participante participante = participanteRepository.findByPublicId(protocoloDto.getId_participante()).orElseGet(()->createParticipante(protocoloDto.getId_participante()));
        Optional<Protocolo> protocoloOptional = protocoloRepository.findByParticipanteIdAndStatusAberto(participante.getId(),StatusProtocolo.ABERTO);
        if(protocoloOptional.isPresent()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe um protocolo em andamento com esse usuário");
        }
        Protocolo protocolo = new Protocolo();
        protocolo.setAdmin(admin);
        protocolo.setParticipante(participante);
        protocolo.setStatus(StatusProtocolo.ABERTO);
        protocolo.setDataCriacao(LocalDateTime.now());
        try {
            protocoloRepository.save(protocolo);
            protocoloRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // A checagem acima (findByParticipanteIdAndStatusAberto) e um check-then-act: duas
            // requisicoes concorrentes para o mesmo participante podem passar por ela antes de
            // qualquer uma persistir. O indice unico uk_protocolo_participante_aberto (ver
            // migration V2026.08.05.20.30.00) e quem garante a invariante de fato; se ele for
            // violado aqui, e porque perdemos essa corrida.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe um protocolo em andamento com esse usuário");
        }
        List<Mensagem> mensagensSemProtocolo = mensagemRepository.findBySenderIdAndProtocoloIsNull(participante.getId());

        for (Mensagem mensagem : mensagensSemProtocolo) {
            mensagem.setProtocolo(protocolo);
        }

        mensagemRepository.saveAll(mensagensSemProtocolo);
        ProtocoloMoveDTO dto = new ProtocoloMoveDTO(protocolo);
        ProtocoloNotificacaoDTO notificacaoDTO = new ProtocoloNotificacaoDTO(participante.getPublicId(), admin.getPublicId());
        Long empresaId = TenantContext.get();
        publishAfterCommit(() -> {
            messagingTemplate.convertAndSend("/topic/empresa/" + empresaId + "/protocolo/aberto/" + protocolo.getParticipante().getPublicId(), dto);
            messagingTemplate.convertAndSend("/topic/empresa/" + empresaId + "/protocolo/novo", notificacaoDTO);
        });
        return dto;
    }

    public Participante createParticipante(String usuarioPublicId){
        Usuario usuario = usuarioRepository.findByPublicId(usuarioPublicId).orElseThrow(()-> new RuntimeException("Nao existe um usuario com esse Id"));
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

    @Transactional
    public void closeProtocolo(String protocolPublicId) {
        Protocolo protocolo = protocoloRepository.findByPublicId(protocolPublicId).orElseThrow(()-> new RuntimeException("Protocolo nao encontrado"));
        if (StatusProtocolo.FECHADO.equals(protocolo.getStatus())) {
            throw new RuntimeException("Protocolo ja encerrado");
        }
        protocolo.setStatus(StatusProtocolo.FECHADO);
        protocolo.setDataEncerramento(LocalDateTime.now());
        protocoloRepository.save(protocolo);

        StatusProtocolo statusFechado = protocolo.getStatus();
        ParticipanteDTO participanteDTO = new ParticipanteDTO(protocolo.getParticipante());
        Long empresaId = TenantContext.get();
        publishAfterCommit(() -> {
            messagingTemplate.convertAndSend("/topic/empresa/" + empresaId + "/protocolo/" + protocolo.getPublicId(), statusFechado);
            messagingTemplate.convertAndSend("/topic/empresa/" + empresaId + "/contatoRet", participanteDTO);
        });
    }

    private void publishAfterCommit(Runnable broadcast) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcast.run();
                }
            });
        } else {
            broadcast.run();
        }
    }

    public Optional<Protocolo> getProtocoloByUsuario(String id_usuario1, String id_usuario2) {
        Usuario usuario1 = usuarioRepository.findByPublicId(id_usuario1)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário 1 não encontrado"));

        Participante participante = participanteRepository.findByPublicId(id_usuario2)
                .orElseThrow(() -> new UsernameNotFoundException("Participante não encontrado"));

        String celular1 = usuario1.getCelular();
        String celular2 = participante.getCelular();

        return protocoloRepository.findByAdminCelularAndParticipanteCelularAndStatus(celular1, celular2, StatusProtocolo.ABERTO);
    }


    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    public List<ProtocoloMoveDTO> getProtocols(String id_usuario) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        boolean isAdmin = isAdmin(authentication);
        Usuario usuario = usuarioRepository.findByPublicId(id_usuario).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));
        if (!isAdmin && !usuario.getLogin().equals(authenticatedUsername)) {
            throw new com.juridiqsystem.crm.service.exceptions.AccessDeniedException("Você não tem permissão para acessar este usuário.");
        }
        // ROLE_ADMIN enxerga todos os protocolos do sistema, não só os que administra/participa
        // pessoalmente (o campo "admin" do protocolo é o atendente responsável pelo chat, um
        // conceito diferente do cargo/autoridade ROLE_ADMIN do usuário autenticado).
        List<Protocolo> protocolos = isAdmin
                ? protocoloRepository.findAllComRelacionamentos()
                : protocoloRepository.findByAdminLoginOrParticipanteLogin(usuario.getLogin());
        if (!protocolos.isEmpty()) {
            return protocolos.stream().map(ProtocoloMoveDTO::new).collect(Collectors.toList());
        } else {
            return null;
        }
    }

    public Page<ProtocoloMoveDTO> getProtocolsPaginado(String id_usuario, String search, Pageable pageable) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        boolean isAdmin = isAdmin(authentication);
        Usuario usuario = usuarioRepository.findByPublicId(id_usuario).orElseThrow(() ->
                new UsernameNotFoundException("Usuário não encontrado"));
        if (!isAdmin && !usuario.getLogin().equals(authenticatedUsername)) {
            throw new com.juridiqsystem.crm.service.exceptions.AccessDeniedException("Você não tem permissão para acessar este usuário.");
        }
        String termoBusca = (search == null || search.isBlank())
                ? null
                : "%" + search.trim().toLowerCase() + "%";
        Page<Protocolo> pagina = isAdmin
                ? protocoloRepository.findAllPaginado(termoBusca, pageable)
                : protocoloRepository.findByAdminLoginOrParticipanteLoginPaginado(usuario.getLogin(), termoBusca, pageable);
        return pagina.map(ProtocoloMoveDTO::new);
    }

    public ProtocoloMoveDTO findById(String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        boolean isAdmin = isAdmin(authentication);
        Optional<Protocolo> protocolo = protocoloRepository.findByPublicId(id);
        if (protocolo.isPresent()) {
            Protocolo p = protocolo.get();
            boolean isAdminAtual = p.getAdmin().getLogin().equals(authenticatedUsername);
            boolean isParticipante = authenticatedUsername.equals(p.getParticipante().getLogin());
            boolean isAdminAnterior = p.getAdminAnterior() != null && p.getAdminAnterior().getLogin().equals(authenticatedUsername);
            if (!isAdmin && !isAdminAtual && !isParticipante && !isAdminAnterior) {
                throw new RuntimeException("Usuário não autorizado a acessar este protocolo.");
            }
            return new ProtocoloMoveDTO(p);
        } else {
            throw new RuntimeException("No content");
        }
    }

    @Transactional
    public ProtocoloMoveDTO encaminha(String id_admin,String id_Protocolo){
        Protocolo protocolo = protocoloRepository.findByPublicId(id_Protocolo).orElseThrow(()->new RuntimeException("Protocolo nao encontrado"));
        Usuario usuario_adm = usuarioRepository.findByPublicId(id_admin)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        Optional<Protocolo> opcional = getProtocoloByUsuario(id_admin,protocolo.getParticipante().getPublicId());
        if (opcional.isPresent()) {
            throw new Error("Já existe um protocolo aberto para este usuário.");
        }
        protocolo.setAdminAnterior(protocolo.getAdmin());
        protocolo.setAdmin(usuario_adm);
        Protocolo protocoloSalvo = protocoloRepository.save(protocolo);

        ProtocoloMoveDTO dto = new ProtocoloMoveDTO(protocoloSalvo);
        ParticipanteDTO participanteDTO = new ParticipanteDTO(protocoloSalvo.getParticipante(), true);
        ProtocoloNotificacao2DTO notify = new ProtocoloNotificacao2DTO(dto,participanteDTO);
        Long empresaId = TenantContext.get();
        publishAfterCommit(() -> messagingTemplate.convertAndSend("/topic/empresa/" + empresaId + "/protocolo/aberto/" + usuario_adm.getPublicId(), notify));
        return dto;
    }


}

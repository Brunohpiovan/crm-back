package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Mensagem;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemResponseDTO;
import br.edu.faculdadevincit.crm_vincit.repository.MensagemRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ProtocoloRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MensagemService{

    @Autowired
    private MensagemRepository mensagemRepository;

    @Autowired
    private ProtocoloRepository protocoloRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CloudFrontService cloudFrontService;


    public List<Mensagem> sendMessage(Protocolo protocolo, Participante sender, String conteudo, String media) {
        List<Mensagem> lista = new ArrayList<>();
        if(conteudo==null || conteudo.isEmpty()){
            conteudo = null;
        }

        Mensagem message = criaMensagem(protocolo, sender, conteudo != null ? conteudo : media);

        if (conteudo != null && media != null) {
            lista.add(mensagemRepository.save(criaMensagem(protocolo, sender, media)));
        }

        if (conteudo != null) {
            message.setConteudo(conteudo);
        }

        if (conteudo != null || media != null) {
            message.setData_envio(LocalDateTime.now());
            lista.add(mensagemRepository.save(message));
        }

        lista.forEach(m -> {
            String msgConteudo = m.getConteudo();
            if (msgConteudo != null && msgConteudo.contains(cloudFrontService.getBaseUrl())) {
                String urlAssinada = cloudFrontService.generateSignedUrl(msgConteudo,Duration.ofMinutes(60));
                m.setConteudo(urlAssinada);
            }
        });

        return lista;
    }

    private Mensagem criaMensagem(Protocolo protocolo, Participante sender, String conteudo){
        Mensagem message = new Mensagem();
        message.setProtocolo(protocolo);
        message.setSender(sender);
        message.setConteudo(conteudo);
        message.setData_envio(LocalDateTime.now());
        return message;
    }

    public Mensagem sendMessagePublico(Participante participante, String body) {
        Mensagem mensagem = new Mensagem();
        mensagem.setSender(participante);
        mensagem.setConteudo(body);
        mensagem.setData_envio(LocalDateTime.now());
        return mensagemRepository.save(mensagem);
    }

    public List<MensagemResponseDTO> getMessagesForProtocolLimit(Long protocoloId, int offset, int limit) {
        Protocolo protocolo = protocoloRepository.findById(protocoloId)
                .orElseThrow(() -> new RuntimeException("Protocolo não encontrado"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        Usuario usuario = (Usuario) usuarioRepository.findByLogin(authenticatedUsername)
                .orElseThrow(() -> new RuntimeException("Admin não encontrado"));

        if (!protocolo.getAdmin().getLogin().equals(usuario.getLogin()) &&
                !protocolo.getParticipante().getLogin().equals(usuario.getLogin())) {
            throw new RuntimeException("Usuário não autorizado a acessar este protocolo.");
        }

        return mensagemRepository
                .findByProtocolo(
                        protocolo,
                        PageRequest.of(offset / limit, limit, Sort.by("id").descending())
                )
                .getContent()
                .stream()
                .map(mensagem -> {
                    MensagemResponseDTO dto = new MensagemResponseDTO(mensagem);

                    String conteudo = dto.getConteudo();
                    if (conteudo != null && conteudo.contains(cloudFrontService.getBaseUrl())) {
                        dto.setConteudo(
                                cloudFrontService.generateSignedUrl(conteudo, Duration.ofMinutes(30))
                        );
                    }

                    return dto;
                })
                .collect(Collectors.toList());

    }


    public List<Mensagem> getMessagesForProtocol(Long protocoloId) {
        Protocolo protocolo = protocoloRepository.findById(protocoloId).orElseThrow(()-> new RuntimeException("Protocolo nao encontrado"));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        Usuario usuario = (Usuario) usuarioRepository.findByLogin(authenticatedUsername)
                .orElseThrow(() -> new RuntimeException("Admin não encontrado"));
        if(!protocolo.getAdmin().getLogin().equals(usuario.getLogin()) && !protocolo.getParticipante().getLogin().equals(usuario.getLogin())){
            throw new RuntimeException("Usuário não autorizado a acessar este protocolo.");
        }

        List<Mensagem> mensagens = mensagemRepository.findByProtocolo(protocolo);
        mensagens.forEach(mensagem -> {
            String conteudo = mensagem.getConteudo();
            if (conteudo != null && conteudo.contains(cloudFrontService.getBaseUrl())) {
                String urlAssinada = cloudFrontService.generateSignedUrl(conteudo, Duration.ofMinutes(30));
                mensagem.setConteudo(urlAssinada);
            }
        });

        return mensagens;

    }

    public List<Mensagem> getMessagesPublic(Long userId) {
        return mensagemRepository.findMessagesWithoutProtocol(userId);
    }

    public List<Long> getSenderIdsWithoutProtocol() {
        return mensagemRepository.findDistinctSenderIdsWithoutProtocol();
    }


    public void deleteMessagesWithoutProtocol(Long userId) {
        List<Mensagem> messagesToDelete = mensagemRepository.findMessagesWithoutProtocol(userId);
        mensagemRepository.deleteAll(messagesToDelete);
    }
}

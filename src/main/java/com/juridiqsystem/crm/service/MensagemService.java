package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Mensagem;
import com.juridiqsystem.crm.model.Participante;
import com.juridiqsystem.crm.model.Protocolo;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.MensagemResponseDTO;
import com.juridiqsystem.crm.repository.MensagemRepository;
import com.juridiqsystem.crm.repository.ParticipanteRepository;
import com.juridiqsystem.crm.repository.ProtocoloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
    private ParticipanteRepository participanteRepository;


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

    public List<Mensagem> sendMessagePublico(Participante participante, String conteudo, String media) {
        List<Mensagem> lista = new ArrayList<>();
        if (conteudo == null || conteudo.isEmpty()) {
            conteudo = null;
        }

        Mensagem message = criaMensagem(null, participante, conteudo != null ? conteudo : media);

        if (conteudo != null && media != null) {
            lista.add(mensagemRepository.save(criaMensagem(null, participante, media)));
        }

        if (conteudo != null) {
            message.setConteudo(conteudo);
        }

        if (conteudo != null || media != null) {
            message.setData_envio(LocalDateTime.now());
            lista.add(mensagemRepository.save(message));
        }

        return lista;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean autorizadoParaProtocolo(Protocolo protocolo, Usuario usuario) {
        boolean isAdminAtual = protocolo.getAdmin().getLogin().equals(usuario.getLogin());
        boolean isParticipante = usuario.getLogin().equals(protocolo.getParticipante().getLogin());
        boolean isAdminAnterior = protocolo.getAdminAnterior() != null
                && protocolo.getAdminAnterior().getLogin().equals(usuario.getLogin());
        return isAdminAtual || isParticipante || isAdminAnterior;
    }

    public List<MensagemResponseDTO> getMessagesForProtocolLimit(String protocoloId, int offset, int limit) {
        Protocolo protocolo = protocoloRepository.findByPublicId(protocoloId)
                .orElseThrow(() -> new RuntimeException("Protocolo não encontrado"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = (Usuario) authentication.getPrincipal();

        if (!autorizadoParaProtocolo(protocolo, usuario)) {
            throw new RuntimeException("Usuário não autorizado a acessar este protocolo.");
        }

        return mensagemRepository
                .findByProtocolo(
                        protocolo,
                        PageRequest.of(offset / limit, limit, Sort.by("id").descending())
                )
                .getContent()
                .stream()
                .map(MensagemResponseDTO::new)
                .collect(Collectors.toList());

    }


    public List<MensagemResponseDTO> getMessagesForProtocol(String protocoloId) {
        Protocolo protocolo = protocoloRepository.findByPublicId(protocoloId).orElseThrow(()-> new RuntimeException("Protocolo nao encontrado"));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = (Usuario) authentication.getPrincipal();
        // Bypass de admin só aqui (tela de detalhes de protocolo/exportação de PDF), não em
        // getMessagesForProtocolLimit (chat ao vivo), para que outro admin não veja mensagens
        // de uma conversa em andamento que não é dele — só o histórico via Protocolos > Detalhes.
        if (!isAdmin(authentication) && !autorizadoParaProtocolo(protocolo, usuario)) {
            throw new RuntimeException("Usuário não autorizado a acessar este protocolo.");
        }

        List<Mensagem> mensagens = mensagemRepository.findByProtocolo(protocolo);

        return mensagens.stream()
                .map(MensagemResponseDTO::new)
                .collect(Collectors.toList());

    }

    public List<MensagemResponseDTO> getMessagesPublic(String userId) {
        Participante participante = participanteRepository.findByPublicId(userId)
                .orElseThrow(() -> new RuntimeException("Participante não encontrado"));
        return mensagemRepository.findBySenderIdAndProtocoloIsNull(participante.getId())
                .stream()
                .map(MensagemResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<String> getSenderIdsWithoutProtocol() {
        return mensagemRepository.findDistinctSenderIdsWithoutProtocol();
    }


    public void deleteMessagesWithoutProtocol(String userId) {
        Participante participante = participanteRepository.findByPublicId(userId)
                .orElseThrow(() -> new RuntimeException("Participante não encontrado"));
        List<Mensagem> messagesToDelete = mensagemRepository.findBySenderIdAndProtocoloIsNull(participante.getId());
        mensagemRepository.deleteAll(messagesToDelete);
    }
}

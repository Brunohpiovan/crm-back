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

    private boolean autorizadoParaProtocolo(Protocolo protocolo, Usuario usuario) {
        boolean isAdminAtual = protocolo.getAdmin().getLogin().equals(usuario.getLogin());
        boolean isParticipante = usuario.getLogin().equals(protocolo.getParticipante().getLogin());
        boolean isAdminAnterior = protocolo.getAdminAnterior() != null
                && protocolo.getAdminAnterior().getLogin().equals(usuario.getLogin());
        return isAdminAtual || isParticipante || isAdminAnterior;
    }

    public List<MensagemResponseDTO> getMessagesForProtocolLimit(Long protocoloId, int offset, int limit) {
        Protocolo protocolo = protocoloRepository.findById(protocoloId)
                .orElseThrow(() -> new RuntimeException("Protocolo não encontrado"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        Usuario usuario = (Usuario) usuarioRepository.findByLogin(authenticatedUsername)
                .orElseThrow(() -> new RuntimeException("Admin não encontrado"));

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


    public List<MensagemResponseDTO> getMessagesForProtocol(Long protocoloId) {
        Protocolo protocolo = protocoloRepository.findById(protocoloId).orElseThrow(()-> new RuntimeException("Protocolo nao encontrado"));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        Usuario usuario = (Usuario) usuarioRepository.findByLogin(authenticatedUsername)
                .orElseThrow(() -> new RuntimeException("Admin não encontrado"));
        if (!autorizadoParaProtocolo(protocolo, usuario)) {
            throw new RuntimeException("Usuário não autorizado a acessar este protocolo.");
        }

        List<Mensagem> mensagens = mensagemRepository.findByProtocolo(protocolo);

        return mensagens.stream()
                .map(MensagemResponseDTO::new)
                .collect(Collectors.toList());

    }

    public List<MensagemResponseDTO> getMessagesPublic(Long userId) {
        return mensagemRepository.findBySenderIdAndProtocoloIsNull(userId)
                .stream()
                .map(MensagemResponseDTO::new)
                .collect(Collectors.toList());
    }

    public List<Long> getSenderIdsWithoutProtocol() {
        return mensagemRepository.findDistinctSenderIdsWithoutProtocol();
    }


    public void deleteMessagesWithoutProtocol(Long userId) {
        List<Mensagem> messagesToDelete = mensagemRepository.findBySenderIdAndProtocoloIsNull(userId);
        mensagemRepository.deleteAll(messagesToDelete);
    }
}

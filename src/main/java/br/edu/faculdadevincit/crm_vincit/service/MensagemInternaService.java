package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.*;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemInternaResponseDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioAllContactsDTO;
import br.edu.faculdadevincit.crm_vincit.repository.ChatGrupoRepository;
import br.edu.faculdadevincit.crm_vincit.repository.MensagemInternaRepository;
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
import java.util.List;

@Service
public class MensagemInternaService {


    @Autowired
    private ChatGrupoRepository chatGrupoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MensagemInternaRepository mensagemInternaRepository;

    @Autowired
    private CloudFrontService cloudFrontService;


    public List<MensagemInternaResponseDTO> getMessagesForProtocolLimit(Long grupoId, int offset, int limit) {
        ChatGrupo grupo = chatGrupoRepository.findById(grupoId)
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authentication.getName();
        Usuario usuario = (Usuario) usuarioRepository.findByLogin(authenticatedUsername)
                .orElseThrow(() -> new RuntimeException("Admin não encontrado"));

        boolean usuarioNoGrupo = grupo.getUsuarios()
                .stream()
                .anyMatch(u -> u.getId().equals(usuario.getId()));

        if (!usuarioNoGrupo) {
            throw new RuntimeException("Usuário não autorizado a acessar este protocolo.");
        }

        return mensagemInternaRepository
                .findByChatGrupo(
                        grupo,
                        PageRequest.of(offset / limit, limit, Sort.by("id").descending())
                )
                .map(mensagem -> {
                    MensagemInternaResponseDTO dto = new MensagemInternaResponseDTO();
                    dto.setId(mensagem.getId());

                    Usuario senderUser = mensagem.getSender();
                    UsuarioAllContactsDTO sender = new UsuarioAllContactsDTO();
                    sender.setId(senderUser.getId());
                    sender.setNome(senderUser.getNome());
                    sender.setUrlPicture(assinarSeForCloudFront(senderUser.getUrlPicture()));

                    dto.setSender(sender);
                    dto.setConteudo(assinarSeForCloudFront(mensagem.getConteudo()));
                    dto.setDataEnvio(mensagem.getDataEnvio());

                    return dto;
                })
                .getContent();
    }

    private String assinarSeForCloudFront(String msg) {
        if (msg != null && msg.contains(cloudFrontService.getBaseUrl())) {
            return cloudFrontService.generateSignedUrl(msg, Duration.ofMinutes(30));
        }
        return msg;
    }

}

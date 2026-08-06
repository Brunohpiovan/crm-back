package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.*;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemInternaResponseDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioAllContactsDTO;
import br.edu.faculdadevincit.crm_vincit.repository.ChatGrupoRepository;
import br.edu.faculdadevincit.crm_vincit.repository.MensagemInternaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MensagemInternaService {


    @Autowired
    private ChatGrupoRepository chatGrupoRepository;

    @Autowired
    private MensagemInternaRepository mensagemInternaRepository;


    public List<MensagemInternaResponseDTO> getMessagesForProtocolLimit(String grupoId, int offset, int limit) {
        ChatGrupo grupo = chatGrupoRepository.findByPublicId(grupoId)
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuario = (Usuario) authentication.getPrincipal();

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
                    dto.setId(mensagem.getPublicId());

                    Usuario senderUser = mensagem.getSender();
                    UsuarioAllContactsDTO sender = new UsuarioAllContactsDTO();
                    sender.setId(senderUser.getPublicId());
                    sender.setNome(senderUser.getNome());
                    sender.setUrlPicture(senderUser.getUrlPicture());

                    dto.setSender(sender);
                    dto.setConteudo(mensagem.getConteudo());
                    dto.setDataEnvio(mensagem.getDataEnvio());

                    return dto;
                })
                .getContent();
    }

}

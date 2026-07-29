package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.ChatGrupo;
import br.edu.faculdadevincit.crm_vincit.model.MensagemInterna;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MensagemInternaResponseDTO {

    private Long id;
    private UsuarioAllContactsDTO sender;
    private Long grupoId;
    private String conteudo;
    private LocalDateTime dataEnvio;

    public MensagemInternaResponseDTO(MensagemInterna mensagemInterna){
        this.id = mensagemInterna.getId();
        this.sender = new UsuarioAllContactsDTO(mensagemInterna.getSender());
        this.grupoId = mensagemInterna.getChatGrupo().getId();
        this.conteudo = mensagemInterna.getConteudo();;
        this.dataEnvio = mensagemInterna.getDataEnvio();

    }
}

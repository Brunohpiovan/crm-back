package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.ChatGrupo;
import br.edu.faculdadevincit.crm_vincit.model.MensagemInterna;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "Mensagem do histórico de chat interno de um grupo.")
@Getter
@Setter
@NoArgsConstructor
public class MensagemInternaResponseDTO {

    private String id;
    @Schema(description = "Dados resumidos do usuário que enviou a mensagem.")
    private UsuarioAllContactsDTO sender;
    @Schema(description = "Id do grupo de chat ao qual a mensagem pertence.")
    private String grupoId;
    @Schema(description = "Conteúdo textual da mensagem.")
    private String conteudo;
    @Schema(description = "Data e hora de envio da mensagem.")
    private LocalDateTime dataEnvio;

    public MensagemInternaResponseDTO(MensagemInterna mensagemInterna){
        this.id = mensagemInterna.getPublicId();
        this.sender = new UsuarioAllContactsDTO(mensagemInterna.getSender());
        this.grupoId = mensagemInterna.getChatGrupo().getPublicId();
        this.conteudo = mensagemInterna.getConteudo();;
        this.dataEnvio = mensagemInterna.getDataEnvio();

    }
}

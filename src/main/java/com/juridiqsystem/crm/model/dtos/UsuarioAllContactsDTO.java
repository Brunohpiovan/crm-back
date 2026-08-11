package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "Resumo de usuário (id, nome, foto) usado nas listagens de contatos disponíveis para chat interno.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAllContactsDTO {

    private String id;
    private String nome;
    private String urlPicture;
    @Schema(description = "Conteúdo da última mensagem trocada com este contato (grupo privado 1-a-1). Nulo em /contacts/dispo, onde ainda não existe grupo entre os dois usuários.")
    private String lastMessage;
    @Schema(description = "Data/hora de envio de `lastMessage`. Nulo se `lastMessage` for nulo.")
    private LocalDateTime lastMessageAt;

    public UsuarioAllContactsDTO(Usuario usuario){
        this.id = usuario.getPublicId();
        this.nome = usuario.getNome();
        this.urlPicture = usuario.getUrlPicture();
    }

    /**
     * Mantido explicitamente (não é mais o gerado por @AllArgsConstructor, que agora tem 5
     * parâmetros) porque UsuarioRepository usa "SELECT new ...UsuarioAllContactsDTO(u.publicId,
     * u.nome, u.urlPicture)" em duas queries JPQL — removê-lo quebraria essas queries em runtime.
     */
    public UsuarioAllContactsDTO(String id, String nome, String urlPicture){
        this.id = id;
        this.nome = nome;
        this.urlPicture = urlPicture;
    }

}

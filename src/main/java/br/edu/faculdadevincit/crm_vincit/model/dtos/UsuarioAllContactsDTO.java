package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Resumo de usuário (id, nome, foto) usado nas listagens de contatos disponíveis para chat interno.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAllContactsDTO {

    private String id;
    private String nome;
    private String urlPicture;

    public UsuarioAllContactsDTO(Usuario usuario){
        this.id = usuario.getPublicId();
        this.nome = usuario.getNome();
        this.urlPicture = usuario.getUrlPicture();
    }

}

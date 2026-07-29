package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAllContactsDTO {

    private Long id;
    private String nome;
    private String urlPicture;

    public UsuarioAllContactsDTO(Usuario usuario){
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.urlPicture = usuario.getUrlPicture();
    }

}

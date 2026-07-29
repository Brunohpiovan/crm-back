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
public class CriadorDto {
    private Long id;
    private String nome;
    private String email;
    private String celular;
    private String urlPicture;

    public CriadorDto(Usuario usuario){
        this.id = usuario.getId();
        this.nome=usuario.getNome();
        this.email = usuario.getLogin();
        this.celular = usuario.getCelular();
        this.urlPicture = usuario.getUrlPicture();
    }
}

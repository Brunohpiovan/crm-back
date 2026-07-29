package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SenderDTO {

    private Long id;
    private String nome;
    private String urlPicture;

    public SenderDTO(Participante usuario){
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.urlPicture = usuario.getUrlPicture();
    }
}

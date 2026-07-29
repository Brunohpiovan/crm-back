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
public class UsuarioAdminDTO {

    private Long id;
    private String nome;

    public UsuarioAdminDTO(Usuario usuario){
        this.id = usuario.getId();
        this.nome = usuario.getNome();
    }
}

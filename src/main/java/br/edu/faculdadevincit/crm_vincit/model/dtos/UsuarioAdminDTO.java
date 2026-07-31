package br.edu.faculdadevincit.crm_vincit.model.dtos;


import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Resumo mínimo (id, nome) do usuário administrador/atendente responsável por um protocolo. Usado embutido em ProtocoloMoveDTO.")
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

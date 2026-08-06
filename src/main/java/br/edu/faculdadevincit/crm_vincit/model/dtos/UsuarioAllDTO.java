package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.enums.Uf;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Schema(description = "Resumo de usuário (id, nome, login, celular, cargo, bloqueado), usado em listagens (busca, administradores).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAllDTO {

    private String id;
    private String nome;
    private String login;
    private String celular;
    @Schema(description = "Cargo do usuário: ADMINISTRADOR concede as autoridades ROLE_ADMIN e ROLE_VENDEDOR; VENDEDOR concede apenas ROLE_VENDEDOR")
    private UserRole cargo;
    @Schema(description = "Se true, o usuário está inativado/bloqueado (não consegue efetuar login)")
    private Boolean bloqueado;


    public UsuarioAllDTO(Usuario usuario){
        this.id = usuario.getPublicId();
        this.nome = usuario.getNome();
        this.login = usuario.getLogin();
        this.celular = usuario.getCelular();
        this.cargo = usuario.getCargo();
        this.bloqueado = usuario.getBloqueado();
    }

}

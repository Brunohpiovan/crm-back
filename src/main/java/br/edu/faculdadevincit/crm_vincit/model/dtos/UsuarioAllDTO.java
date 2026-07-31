package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.enums.Uf;
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

@Schema(description = "Resumo de usuário (id, nome, login, celular), usado em listagens (busca, administradores).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAllDTO {

    private Long id;
    private String nome;
    private String login;
    private String celular;


    public UsuarioAllDTO(Usuario usuario){
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.login = usuario.getLogin();
        this.celular = usuario.getCelular();
    }

}

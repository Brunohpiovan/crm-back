package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.enums.Uf;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Schema(description = "Dados completos do usuário, retornados por GET /usuario/{id} (apenas para o próprio usuário autenticado).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDto {

    private Long id;
    private String urlPicture;
    @NotNull(message = "O campo NOME é requerido.")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    private String nome;
    @Email(message = "O email informado é inválido")
    @NotNull(message = "O campo LOGIN é requerido.")
    private String login;
    @NotNull(message = "O campo RG é requerido.")
    private String rg;
    @NotNull(message = "O campo CPF é requerido.")
    private String cpf;
    @NotNull(message = "O campo DATA DE NASCIMENTO é requerido.")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate dataNascimento;
    @NotNull(message = "O campo TELEFONE é requerido.")
    private String celular;
    @NotNull(message = "O campo ENDERECO é requerido.")
    private String endereco;
    @NotNull(message = "O campo NUMERO RESIDENCIAL é requerido.")
    private String numeroResidencial;
    private String complemento;
    @NotNull(message = "O campo BAIRRO é requerido.")
    private String bairro;
    @NotNull(message = "O campo UF é requerido.")
    @Enumerated(EnumType.STRING)
    private Uf uf;
    @NotNull(message = "O campo CIDADE é requerido.")
    private String cidade;
    @NotNull(message = "O campo CEP é requerido.")
    private String cep;
    private String observacoes;
    @Schema(description = "Cargo do usuário, como texto (ex.: \"ADMINISTRADOR\", \"VENDEDOR\")")
    private String cargo;
    @Schema(description = "Se true, o usuário está inativado/bloqueado (não consegue efetuar login)")
    private Boolean bloqueado;

    public UsuarioResponseDto(Usuario usuario){
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.urlPicture=usuario.getUrlPicture();
        this.login = usuario.getLogin();
        this.rg = usuario.getRg();
        this.cpf = usuario.getCpf();
        this.dataNascimento = usuario.getDataNascimento();
        this.celular = usuario.getCelular();
        this.endereco = usuario.getEndereco();
        this.numeroResidencial = usuario.getNumeroResidencial();
        this.complemento = usuario.getComplemento();
        this.bairro = usuario.getBairro();
        this.uf = usuario.getUf();
        this.cidade = usuario.getCidade();
        this.observacoes = usuario.getObservacoes();
        this.cargo = usuario.getCargo().toString();
        this.cep = usuario.getCep().toString();
        this.bloqueado = usuario.getBloqueado();
    }
}

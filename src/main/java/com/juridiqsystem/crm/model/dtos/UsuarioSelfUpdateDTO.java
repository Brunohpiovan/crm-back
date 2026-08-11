package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.enums.Uf;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
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

/**
 * Payload de atualização do próprio cadastro (PUT /usuario/{id}).
 * Não possui cargo nem bloqueado: essas propriedades administrativas só
 * podem ser alteradas através de UsuarioAdminUpdateDTO (PUT /usuario/all/{id}).
 */
@Schema(description = "Payload multipart (parte 'usuario') para o próprio usuário atualizar seus dados pessoais (PUT /usuario/{id}). Não permite alterar cargo nem bloqueado.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioSelfUpdateDTO implements UsuarioDadosPessoaisDTO {

    @Schema(description = "URL de foto/avatar já existente, se aplicável (normalmente omitido; a foto é enviada como arquivo na parte 'foto')")
    @Nullable
    private String urlPicture;

    @NotNull(message = "O campo NOME é requerido.")
    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    private String nome;

    @Email(message = "O email informado é inválido")
    @NotNull(message = "O campo LOGIN é requerido.")
    @Size(max = 150, message = "O login deve ter no maximo 255 caracteres")
    private String login;

    @Schema(description = "Ignorado por este endpoint — troca de senha não é mais feita aqui. Use PUT /usuario/senha (exige a senha atual).")
    @Size(min = 8, max = 255, message = "A Senha deve ter entre 6 e 100 caracteres")
    private String senha;

    @Schema(description = "Ignorado por este endpoint — troca de senha não é mais feita aqui. Use PUT /usuario/senha (exige a senha atual).")
    @Size(min = 8, max = 255, message = "A senha de confirmação deve ter entre 6 e 100 caracteres")
    private String senha2;

    @NotNull(message = "O campo RG é requerido.")
    @Size(max = 12, message = "O rg deve ter no maximo 12 caracteres")
    private String rg;

    @NotNull(message = "O campo CPF é requerido.")
    @Size(max = 14, message = "O cpf deve ter no maximo 14 caracteres")
    private String cpf;

    @NotNull(message = "O campo DATA DE NASCIMENTO é requerido.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate dataNascimento;

    @NotNull(message = "O campo TELEFONE é requerido.")
    @Size(max = 15, message = "O telefone deve ter no maximo 15 caracteres")
    private String celular;

    @NotNull(message = "O campo ENDERECO é requerido.")
    @Size(max = 255, message = "O endereco deve ter no maximo 255 caracteres")
    private String endereco;

    @NotNull(message = "O campo NUMERO RESIDENCIAL é requerido.")
    @Size(max = 50, message = "O numero residencial deve ter no maximo 50 caracteres")
    private String numeroResidencial;

    @Size(max = 255, message = "O complemento deve ter no maximo 255 caracteres")
    private String complemento;

    @NotNull(message = "O campo BAIRRO é requerido.")
    @Size(max = 100, message = "O campo bairro deve ter no maximo 100 caracteres")
    private String bairro;

    @Schema(description = "Sigla da unidade federativa (UF) brasileira")
    @NotNull(message = "O campo UF é requerido.")
    @Enumerated(EnumType.STRING)
    private Uf uf;

    @NotNull(message = "O campo CIDADE é requerido.")
    @Size(max = 100, message = "O campo cidade deve ter no maximo 100 caracteres")
    private String cidade;

    @NotNull(message = "Informe um cep")
    @Size(max = 10, message = "O cep deve ter no maximo 10 caracteres")
    private String cep;

    private String observacoes;
}

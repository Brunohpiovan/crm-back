package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Payload para o próprio usuário trocar a senha (PUT /master/senha).")
@Getter
@Setter
@NoArgsConstructor
public class AlterarSenhaDTO {

    @NotNull(message = "Informe a senha atual")
    private String senhaAtual;

    @NotNull(message = "Informe a nova senha")
    @Size(min = 8, max = 255, message = "A nova senha deve ter entre 8 e 255 caracteres")
    private String novaSenha;

    @NotNull(message = "Confirme a nova senha")
    private String novaSenha2;
}

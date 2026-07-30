package br.edu.faculdadevincit.crm_vincit.model.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OportunidadeClienteRequest(
        Long id,
        @NotBlank(message = "Informe um nome") @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres") String nome,
        @Size(max = 150, message = "O login deve ter no maximo 150 caracteres") String login,
        @NotBlank(message = "Informe um celular") @Size(max = 15, message = "O celular deve ter no maximo 15 caracteres") String celular
) {
}

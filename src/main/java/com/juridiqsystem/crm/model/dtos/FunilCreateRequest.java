package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para criar um novo funil, enviados como corpo de POST /funil.")
public record FunilCreateRequest(
        @Schema(description = "Nome do funil", example = "Vendas")
        @NotBlank(message = "Informe um nome")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
        String nome
) {
}

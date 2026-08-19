package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para criar uma nova etapa, enviados como corpo de POST /etapa.")
public record EtapaCreateRequest(
        @NotBlank(message = "Informe um nome") @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres") String nome,
        @Schema(description = "Funil ao qual a etapa vai pertencer (apenas o id é considerado)")
        @NotNull(message = "Informe o funil") @Valid FunilDtoCard funil
) {
}

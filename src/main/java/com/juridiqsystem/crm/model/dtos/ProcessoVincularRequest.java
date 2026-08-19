package com.juridiqsystem.crm.model.dtos;

import jakarta.validation.constraints.NotBlank;

public record ProcessoVincularRequest(
        @NotBlank(message = "O número CNJ é obrigatório") String numeroCnj
) {
}

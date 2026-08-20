package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.enums.Uf;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload para ligar o monitoramento de uma OAB nos Diários Oficiais.")
public record IntimacaoMonitoramentoAtivarRequest(

        @NotBlank(message = "Informe o número da OAB.")
        String oabNumero,

        @NotNull(message = "Informe a UF da OAB.")
        Uf oabUf,

        @Schema(description = "publicId do usuário/advogado do escritório dono desta OAB. Opcional — vínculo só informativo (filtro/exibição), nunca usado para autorização.")
        String usuarioAdvogadoId) {
}

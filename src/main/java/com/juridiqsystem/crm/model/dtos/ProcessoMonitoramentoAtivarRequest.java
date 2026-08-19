package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.enums.FrequenciaMonitoramento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload para ligar o monitoramento contínuo de um processo.")
public record ProcessoMonitoramentoAtivarRequest(

        @Schema(description = "Cadência de verificação nos tribunais/diários. DIARIA (seg-sex) ou SEMANAL.", example = "DIARIA")
        @NotNull(message = "Informe a frequência do monitoramento")
        FrequenciaMonitoramento frequencia,

        @Schema(description = "Se true, também busca documentos públicos do processo. Custa mais por processo monitorado.", example = "false")
        @NotNull(message = "Informe se o monitoramento deve incluir documentos")
        Boolean comDocumentos) {
}

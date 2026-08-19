package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cota de processos monitorados simultaneamente do plano da empresa.")
public record MonitoramentoQuotaResponse(

        @Schema(description = "Limite do plano. Null significa ilimitado.", example = "50")
        Integer limite,

        @Schema(description = "Quantidade de processos com monitoramento ligado agora.", example = "12")
        long utilizados) {
}

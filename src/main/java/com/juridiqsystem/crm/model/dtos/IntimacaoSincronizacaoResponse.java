package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de uma sincronização manual de aparições (rede de segurança para quando o callback falha).")
public record IntimacaoSincronizacaoResponse(

        @Schema(description = "Quantas intimações novas foram encontradas e registradas — 0 não é erro, só significa que nada de novo apareceu.", example = "3")
        int novasEncontradas) {
}

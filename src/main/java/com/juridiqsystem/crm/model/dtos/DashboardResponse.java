package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Payload completo consumido por GET /dashboard — uma única requisição carrega todos os blocos da tela.")
public record DashboardResponse(
        DashboardSummaryResponse summary,
        @Schema(description = "Etapas do funil de vendas, na ordem do pipeline") List<DashboardFunilEtapaResponse> funil,
        @Schema(description = "Origem dos leads (oportunidades) no período") List<DashboardOrigemResponse> origensLead,
        @Schema(description = "Série diária de protocolos abertos/fechados, um ponto por dia do período") List<DashboardSerieDiariaResponse> serieDiariaProtocolos,
        @Schema(description = "Ranking de usuários por valor vendido, decrescente") List<DashboardRankingResponse> ranking,
        DashboardCadenciaResponse cadencias
) {
}

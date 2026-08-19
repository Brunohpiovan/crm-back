package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Panorama das cadências de funil visíveis para o usuário autenticado.")
public record DashboardCadenciaResponse(
        @Schema(description = "Quantidade de cadências com situação ATIVA") Long ativas,
        @Schema(description = "Quantidade de cadências com situação INATIVA") Long pausadas,
        @Schema(description = "Quantidade de movimentações automáticas de cadência (scheduler) executadas hoje, para as cadências ativas visíveis — valor exato, lido de log_movimentacao_cadencia (não conta movimentação manual/drag-and-drop).") Long execucoesHoje,
        @Schema(description = "Quantidade de oportunidades atualmente na etapa de origem de alguma cadência ativa (candidatas à próxima movimentação automática)") Long oportunidadesEmExecucao,
        @Schema(description = "Data/hora da próxima execução programada entre todas as cadências ativas visíveis (hoje ou amanhã, no horarioMovimentacao mais próximo ainda não passado hoje; null se não houver cadência ativa)") LocalDateTime proximaExecucao
) {
}

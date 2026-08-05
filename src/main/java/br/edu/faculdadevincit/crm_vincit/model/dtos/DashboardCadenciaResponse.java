package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Panorama das cadências de funil visíveis para o usuário autenticado.")
public record DashboardCadenciaResponse(
        @Schema(description = "Quantidade de cadências com situação ATIVA") Long ativas,
        @Schema(description = "Quantidade de cadências com situação INATIVA") Long pausadas,
        @Schema(description = "Aproximação: quantidade de oportunidades cuja etapa atual é a etapa de destino de alguma cadência ativa e cuja dataEntradaEtapa é hoje. Também conta movimentações manuais para a mesma etapa no dia — não há tabela de auditoria de execuções do scheduler no schema atual.") Long execucoesHoje,
        @Schema(description = "Quantidade de oportunidades atualmente na etapa de origem de alguma cadência ativa (candidatas à próxima movimentação automática)") Long oportunidadesEmExecucao,
        @Schema(description = "Data/hora da próxima execução programada entre todas as cadências ativas visíveis (hoje ou amanhã, no horarioMovimentacao mais próximo ainda não passado hoje; null se não houver cadência ativa)") LocalDateTime proximaExecucao
) {
}

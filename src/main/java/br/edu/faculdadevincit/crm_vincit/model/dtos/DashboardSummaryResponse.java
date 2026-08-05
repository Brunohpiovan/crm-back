package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Indicadores agregados do período filtrado (padrão: últimos 30 dias), comparados ao período anterior de mesma duração.")
public record DashboardSummaryResponse(
        @Schema(description = "Soma do valor das oportunidades com situação ABERTO no período") BigDecimal valorEmNegociacao,
        @Schema(description = "Variação percentual de valorEmNegociacao em relação ao período anterior de mesma duração (positivo = alta, negativo = queda, null se o período anterior não teve valor para comparar)") Double variacaoPercentual,
        @Schema(description = "Percentual de oportunidades GANHO em relação a GANHO+PERDIDO no período (0 se não houve oportunidades fechadas)") Double taxaConversao,
        @Schema(description = "Quantidade de protocolos com status ABERTO no período") Long protocolosAbertos,
        @Schema(description = "Quantidade de protocolos ABERTO cuja dataCriacao já ultrapassou o limite de horas configurado (dashboard.protocolo.risco-horas)") Long protocolosEmRisco,
        @Schema(description = "Tempo médio, em minutos, entre dataCriacao e dataEncerramento dos protocolos FECHADO no período (null se não houve protocolos fechados)") Double tempoMedioAtendimentoMinutos,
        @Schema(description = "Quantidade de oportunidades com situação ABERTO no período") Long oportunidadesAbertas,
        @Schema(description = "Quantidade de oportunidades com situação GANHO no período") Long oportunidadesGanhas,
        @Schema(description = "Quantidade de oportunidades com situação PERDIDO no período") Long oportunidadesPerdidas
) {
}

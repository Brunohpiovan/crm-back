package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Estatísticas de desempenho de um usuário no período filtrado, ordenadas por valorVendido decrescente.")
public record DashboardRankingResponse(
        @Schema(description = "Id do usuário") Long usuarioId,
        @Schema(description = "Nome do usuário") String usuarioNome,
        @Schema(description = "Quantidade de protocolos FECHADO em que o usuário foi o admin/atendente") Long protocolosFechados,
        @Schema(description = "Tempo médio, em minutos, dos protocolos fechados pelo usuário (null se não houve nenhum)") Double tempoMedioAtendimentoMinutos,
        @Schema(description = "Quantidade de oportunidades GANHO em que o usuário foi o criador/responsável") Long oportunidadesGanhas,
        @Schema(description = "Quantidade de oportunidades PERDIDO em que o usuário foi o criador/responsável") Long oportunidadesPerdidas,
        @Schema(description = "Soma do valor das oportunidades GANHO do usuário") BigDecimal valorVendido,
        @Schema(description = "Percentual de GANHO em relação a GANHO+PERDIDO do usuário (0 se não houve oportunidades fechadas)") Double taxaConversao
) {
}

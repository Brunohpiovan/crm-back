package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Ponto diário de série histórica: protocolos abertos/fechados, valor de oportunidades ABERTO criadas no dia e tempo médio de atendimento dos protocolos fechados no dia. Usado tanto pelo gráfico principal de protocolos quanto pelos mini-gráficos dos cards de KPI.")
public record DashboardSerieDiariaResponse(
        @Schema(description = "Data do ponto da série") LocalDate data,
        @Schema(description = "Quantidade de protocolos com dataCriacao neste dia") Long protocolosAbertos,
        @Schema(description = "Quantidade de protocolos com dataEncerramento neste dia") Long protocolosFechados,
        @Schema(description = "Soma do valor das oportunidades com situação ABERTO cuja dataCriacao é este dia (proxy de atividade — não é o saldo total em aberto na data, que o schema atual não permite reconstruir historicamente; ver DASHBOARD.md)") BigDecimal valorEmNegociacao,
        @Schema(description = "Tempo médio, em minutos, entre dataCriacao e dataEncerramento dos protocolos FECHADO cuja dataEncerramento é este dia (null se nenhum protocolo foi fechado neste dia)") Double tempoMedioAtendimentoMinutos
) {
}

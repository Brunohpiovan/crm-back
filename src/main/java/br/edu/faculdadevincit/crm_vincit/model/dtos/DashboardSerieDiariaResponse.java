package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Quantidade de protocolos abertos e fechados em um dia específico.")
public record DashboardSerieDiariaResponse(
        @Schema(description = "Data do ponto da série") LocalDate data,
        @Schema(description = "Quantidade de protocolos com dataCriacao neste dia") Long protocolosAbertos,
        @Schema(description = "Quantidade de protocolos com dataEncerramento neste dia") Long protocolosFechados
) {
}

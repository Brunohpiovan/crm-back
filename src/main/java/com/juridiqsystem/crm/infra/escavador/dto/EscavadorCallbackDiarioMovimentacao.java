package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Objeto {@code movimentacao} do callback {@code diario_movimentacao_nova} (§9.13 — "case movement
 * info"; formato exato **não confirmado** na documentação, apenas o outline dos campos). Assumimos
 * a mesma forma de {@link EscavadorCallbackMovimentacao} (data/tipo/conteudo), por ser o padrão já
 * observado nos demais callbacks da Escavador — ajustar se
 * POST /api/v1/monitoramentos/{id}/testcallback revelar um formato diferente em produção.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorCallbackDiarioMovimentacao(
        String data,
        String tipo,
        String conteudo) {
}

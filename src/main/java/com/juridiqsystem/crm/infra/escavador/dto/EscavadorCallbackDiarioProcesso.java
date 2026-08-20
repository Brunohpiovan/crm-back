package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Objeto {@code processo} do callback {@code diario_movimentacao_nova} (§9.13 — formato exato
 * **não confirmado**). Aceita tanto {@code numero} (nome usado em EscavadorMonitoramentoResponse)
 * quanto {@code numero_cnj} (nome usado em EscavadorProcesso) via @JsonAlias, defensivamente, até
 * confirmar contra um payload real (POST /api/v1/monitoramentos/{id}/testcallback).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorCallbackDiarioProcesso(
        @JsonAlias("numero_cnj") String numero) {
}

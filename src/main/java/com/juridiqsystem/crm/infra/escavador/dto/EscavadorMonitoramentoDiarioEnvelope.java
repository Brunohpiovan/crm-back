package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Envelope real de POST /api/v1/monitoramentos e GET /api/v1/monitoramentos/{id} —
 * {@code {"status": "success", "monitoramento": {...}}}. A documentação local sugeria um objeto
 * solto no nível raiz (sem esse envelope); confirmado ao vivo contra a conta real que ambos os
 * endpoints embrulham a resposta assim. Ver EscavadorMonitoramentoDiarioResponse.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorMonitoramentoDiarioEnvelope(
        String status,
        EscavadorMonitoramentoDiarioResponse monitoramento) {
}

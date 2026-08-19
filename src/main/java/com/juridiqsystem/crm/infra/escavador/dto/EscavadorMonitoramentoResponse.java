package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta de POST/GET /api/v2/monitoramentos/processos (e o objeto {@code monitoramento} que vem
 * dentro de todo callback). Só mapeamos os campos que usamos — @JsonIgnoreProperties evita que a
 * inclusão de um campo novo pela Escavador quebre a desserialização.
 *
 * @param status PENDENTE (recém-criado), ENCONTRADO (robô localizou o processo e está monitorando)
 *               ou NAO_ENCONTRADO (não há cobrança neste caso).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorMonitoramentoResponse(
        Long id,
        String numero,
        String frequencia,
        String status,
        @JsonProperty("documentos_publicos") Boolean documentosPublicos) {
}

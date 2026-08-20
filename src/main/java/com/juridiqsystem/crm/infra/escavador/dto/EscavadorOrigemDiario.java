package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Um diário oficial dentro de um {@link EscavadorOrigemGrupo}, devolvido por GET /api/v1/origens
 * (§9.5). {@code estado} é a UF de competência do diário (null/vazio para diários de âmbito
 * nacional/superior) e {@code categoria} indica o tipo de tribunal/órgão — ambos usados pela
 * heurística de resolução automática de origem_ids em IntimacaoMonitoramentoService.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorOrigemDiario(
        Integer id,
        String nome,
        String sigla,
        String estado,
        String categoria) {
}

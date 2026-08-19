package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Corpo de POST /api/v2/monitoramentos/processos. Os nomes em snake_case são os da API da
 * Escavador — este record é a única tradução entre o nosso modelo e o contrato do provedor.
 *
 * @param numero             numeração CNJ do processo (único campo obrigatório).
 * @param tribunal           sigla do tribunal a monitorar; null deixa a Escavador usar o tribunal de origem.
 * @param frequencia         DIARIA ou SEMANAL (ver FrequenciaMonitoramento).
 * @param documentosPublicos se true, o monitoramento também busca documentos públicos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EscavadorMonitoramentoCreateRequest(
        String numero,
        String tribunal,
        String frequencia,
        @JsonProperty("documentos_publicos") Boolean documentosPublicos) {
}

package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Corpo de POST /api/v1/monitoramentos (§9.4 "Monitoramento de Diários Oficiais") — nomes em
 * snake_case são os da API da Escavador.
 *
 * <p>A doc local (escavador-api.md) descreve o campo como {@code origem_ids} e o valor de
 * {@code tipo} como {@code "TERMO"} (maiúsculo) — ambos incorretos na prática: a API real rejeita
 * essa forma com 422 ("O campo estados ids é obrigatório quando (...) origens ids não está
 * presente" + "A opção de tipo selecionada é inválida"). Confirmado contra o código-fonte do SDK
 * oficial Python (github.com/Escavador/escavador-python,
 * escavador/v1/resources/monitoramento_diario.py + enums_v1.TiposMonitoramentosDiario): o campo é
 * {@code origens_ids} (plural) e {@code tipo} é minúsculo ({@code "termo"}/{@code "processo"}).
 *
 * @param termo      termo monitorado (aqui, sempre o número da OAB).
 * @param origensIds ids dos diários a monitorar (ver EscavadorMonitoramentoDiarioApi.listarOrigens
 *                   e a heurística de resolução por UF em IntimacaoMonitoramentoService).
 * @param tipo       sempre "termo" neste prompt (o outro valor aceito, "processo", não é usado —
 *                   monitoramos OAB, não processo específico).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EscavadorMonitoramentoDiarioCreateRequest(
        String termo,
        @JsonProperty("origens_ids") List<Integer> origensIds,
        String tipo) {

    public static EscavadorMonitoramentoDiarioCreateRequest termo(String termo, List<Integer> origensIds) {
        return new EscavadorMonitoramentoDiarioCreateRequest(termo, origensIds, "termo");
    }
}

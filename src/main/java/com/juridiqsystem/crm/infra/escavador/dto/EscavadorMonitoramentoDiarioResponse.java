package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta de POST/GET /api/v1/monitoramentos (e o(s) objeto(s) {@code monitoramento} que vem
 * dentro dos callbacks {@code diario_movimentacao_nova}/{@code diario_citacao_nova}). Só mapeamos
 * os campos que usamos — o exemplo de resposta do POST na documentação é mais enxuto que o de
 * "listar monitoramentos" (que também traz {@code descricao}/{@code processo}), então nem todo
 * campo abaixo vem preenchido em toda resposta; @JsonIgnoreProperties evita quebrar em qualquer um
 * dos dois formatos.
 *
 * <p>Diferente do documentado (que sugeria um objeto solto no nível raiz), tanto POST quanto
 * GET /monitoramentos/{id} devolvem o objeto embrulhado em {@link EscavadorMonitoramentoDiarioEnvelope}
 * — confirmado ao vivo contra a conta real (curl em {@code GET /api/v1/monitoramentos/{id}}
 * devolveu {@code {"status":"success","monitoramento":{"id":...,...}}}). Sem o unwrap, {@code id}
 * sempre desserializava null e a criação nunca era confirmada, mesmo tendo sido criada de verdade
 * na Escavador (e cobrada) — ver EscavadorMonitoramentoDiarioApi.criar.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorMonitoramentoDiarioResponse(
        Long id,
        String termo,
        String tipo,
        @JsonProperty("qtd_aparicoes") Integer qtdAparicoes,
        @JsonProperty("numero_diarios_monitorados") Integer numeroDiariosMonitorados,
        @JsonProperty("data_ultima_aparicao") String dataUltimaAparicao) {
}

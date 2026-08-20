package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Objeto {@code diario} do callback {@code diario_citacao_nova} (§9.13 — formato exato **não
 * confirmado**, apenas descrito como "diario object"). {@code id} é o pedaço que falta, junto com
 * {@code pagina_diario.pagina}, para compor a chave de dedupe da intimação
 * (monitoramento_id+diario_id+página) — ver EscavadorCallbackDiarioPagina e IntimacaoService.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorCallbackDiarioDiario(
        Long id,
        String nome,
        String sigla,
        @JsonAlias({"data_publicacao", "data"}) String data,
        String link) {
}

package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Item de {@code GET /processos/numero_cnj/{numero}/documentos} (§8.4) — documentos públicos do
 * processo. {@code [unconfirmed]}: a doc local não detalha o JSON exato desse endpoint; o shape
 * usado aqui é o mesmo do objeto {@code documento} do callback {@code novo_documento} (§8.9, o
 * melhor palpite disponível). Confirme contra a API real antes de depender de nomes de campo além
 * dos já usados. {@code key} é o valor a usar como {@code documento_id} no download do PDF.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorDocumento(
        Long id,
        String titulo,
        String descricao,
        String data,
        String tipo,
        @JsonProperty("extensao_arquivo") String extensaoArquivo,
        @JsonProperty("quantidade_paginas") Integer quantidadePaginas,
        String key,
        EscavadorDocumentoLinks links,
        @JsonProperty("vencimento_link") String vencimentoLink) {
}

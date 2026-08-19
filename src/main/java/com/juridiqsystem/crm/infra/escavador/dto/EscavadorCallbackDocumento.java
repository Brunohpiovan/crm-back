package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Objeto {@code documento} do callback {@code novo_documento} (§8.9). {@code key} é o valor a
 * plugar em {@code {documento_id}} no download do PDF (§8.4); o link temporário embutido no
 * callback expira em {@code vencimento_link} — por isso o download é sempre refeito sob demanda
 * via {@code EscavadorProcessoApi.baixarDocumentoPdf}, nunca lido diretamente daqui.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorCallbackDocumento(
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

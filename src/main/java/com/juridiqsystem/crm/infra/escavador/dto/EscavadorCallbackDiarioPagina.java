package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Objeto {@code pagina_diario} do callback {@code diario_citacao_nova} (§9.13 — "page number + HTML
 * content", formato exato **não confirmado**). {@code conteudo} é tratado como o HTML/texto da
 * página; se a Escavador enviar um campo com nome diferente, ele fica perdido silenciosamente aqui
 * até o parser ser ajustado contra um payload real de produção — não quebra o processamento (o
 * evento ainda é registrado em EscavadorCallbackEvento com o payload bruto).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorCallbackDiarioPagina(
        Integer pagina,
        String conteudo,
        String link) {
}

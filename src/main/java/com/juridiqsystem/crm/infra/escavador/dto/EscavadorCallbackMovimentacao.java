package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Objeto {@code movimentacao} do callback {@code nova_movimentacao}.
 *
 * @param data     data da movimentação, no formato "yyyy-MM-dd" (a Escavador não envia hora aqui).
 * @param tipo     ANDAMENTO (veio do sistema do tribunal) ou PUBLICACAO (veio de diário oficial).
 * @param conteudo texto da movimentação.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorCallbackMovimentacao(
        Long id,
        String data,
        String tipo,
        String conteudo) {
}

package com.juridiqsystem.crm.infra.escavador.dto;

public record EscavadorMovimentacao(
        Long id,
        String data,
        String tipo,
        String conteudo,
        EscavadorMovimentacaoFonte fonte
) {
}

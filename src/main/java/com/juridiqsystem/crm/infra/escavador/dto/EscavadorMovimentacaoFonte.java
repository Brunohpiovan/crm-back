package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EscavadorMovimentacaoFonte(
        @JsonProperty("fonte_id") Long fonteId,
        String nome,
        String tipo,
        String sigla,
        Integer grau,
        @JsonProperty("grau_formatado") String grauFormatado
) {
}

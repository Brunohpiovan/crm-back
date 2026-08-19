package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EscavadorValorCausa(String valor, String moeda, @JsonProperty("valor_formatado") String valorFormatado) {
}

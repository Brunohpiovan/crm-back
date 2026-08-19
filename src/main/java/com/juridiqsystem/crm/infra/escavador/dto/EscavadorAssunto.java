package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EscavadorAssunto(
        Long id,
        String nome,
        @JsonProperty("nome_com_pai") String nomeComPai,
        @JsonProperty("path_completo") String pathCompleto,
        @JsonProperty("categoria_raiz") String categoriaRaiz,
        Boolean bloqueado
) {
}

package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EscavadorUnidadeOrigem(
        String nome,
        String endereco,
        String classificacao,
        String cidade,
        EscavadorEstado estado,
        @JsonProperty("tribunal_sigla") String tribunalSigla
) {
}

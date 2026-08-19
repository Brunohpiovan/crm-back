package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Resposta de "GET /api/v2/processos/numero_cnj/{numero}/ia/resumo". */
public record EscavadorResumoIa(
        @JsonProperty("numero_cnj") String numeroCnj,
        String conteudo,
        @JsonProperty("atualizado_em") String atualizadoEm
) {
}

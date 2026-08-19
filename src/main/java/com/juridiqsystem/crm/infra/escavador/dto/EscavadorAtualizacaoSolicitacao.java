package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Resposta de "POST /api/v2/processos/numero_cnj/{numero}/solicitar-atualizacao" (§8.3). */
public record EscavadorAtualizacaoSolicitacao(
        Long id,
        String status,
        @JsonProperty("numero_cnj") String numeroCnj,
        @JsonProperty("criado_em") String criadoEm,
        @JsonProperty("concluido_em") String concluidoEm
) {
}

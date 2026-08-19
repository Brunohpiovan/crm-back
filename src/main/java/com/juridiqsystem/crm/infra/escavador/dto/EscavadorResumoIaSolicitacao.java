package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Resposta de "POST .../ia/resumo/solicitar-atualizacao" e de "GET .../ia/resumo/status" (mesmo formato). */
public record EscavadorResumoIaSolicitacao(
        Long id,
        String status,
        @JsonProperty("criado_em") String criadoEm,
        @JsonProperty("numero_cnj") String numeroCnj,
        @JsonProperty("concluido_em") String concluidoEm,
        @JsonProperty("enviar_callback") String enviarCallback
) {
}

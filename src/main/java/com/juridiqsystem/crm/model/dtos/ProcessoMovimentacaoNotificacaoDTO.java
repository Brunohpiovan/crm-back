package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Payload publicado em /topic/empresa/{empresaId}/processo/{processoPublicId}/movimentacao.
 * Deliberadamente mínimo: é um aviso de "há novidade", não a novidade em si — o frontend refaz o
 * fetch da timeline ao receber. Assim o WebSocket nunca vira um canal alternativo de leitura de
 * dado de processo (que teria de repetir toda a autorização já feita no endpoint REST).
 */
@Schema(description = "Aviso de que novas movimentações foram detectadas em um processo.")
public record ProcessoMovimentacaoNotificacaoDTO(
        String processoPublicId,
        int quantidade,
        LocalDateTime ocorridoEm) {
}

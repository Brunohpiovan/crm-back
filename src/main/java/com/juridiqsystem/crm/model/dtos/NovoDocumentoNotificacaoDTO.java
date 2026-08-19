package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Payload publicado em /topic/empresa/{empresaId}/documento. Deliberadamente mínimo, mesmo
 * racional de ProcessoMovimentacaoNotificacaoDTO: é um aviso de "há novidade", o frontend refaz o
 * fetch da lista de documentos ao receber.
 */
@Schema(description = "Aviso de que um novo documento foi detectado em um processo.")
public record NovoDocumentoNotificacaoDTO(
        String processoPublicId,
        String tituloDocumento,
        LocalDateTime ocorridoEm) {
}

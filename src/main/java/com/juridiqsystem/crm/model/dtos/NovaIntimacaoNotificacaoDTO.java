package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Payload publicado em /topic/empresa/{empresaId}/intimacao. Deliberadamente mínimo, mesmo
 * racional de NovoDocumentoNotificacaoDTO: é um aviso de "há novidade", o frontend refaz o fetch
 * da lista de intimações ao receber.
 */
@Schema(description = "Aviso de que uma nova intimação/publicação foi detectada para uma OAB monitorada.")
public record NovaIntimacaoNotificacaoDTO(
        Long intimacaoId,
        String oabNumero,
        String diarioNome,
        LocalDateTime ocorridoEm) {
}

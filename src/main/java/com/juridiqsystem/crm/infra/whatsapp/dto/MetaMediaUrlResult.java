package com.juridiqsystem.crm.infra.whatsapp.dto;

/** Resultado de GET /{media-id}: a Graph API devolve uma URL temporária e autenticada, não o binário direto. */
public record MetaMediaUrlResult(String url, String mimeType) {
}

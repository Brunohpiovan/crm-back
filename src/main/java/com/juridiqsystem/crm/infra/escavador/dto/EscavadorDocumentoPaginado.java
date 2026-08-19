package com.juridiqsystem.crm.infra.escavador.dto;

import java.util.List;

/** Envelope items/links/paginator (ver docs §Pagination) para {@code GET .../documentos}. */
public record EscavadorDocumentoPaginado(List<EscavadorDocumento> items, EscavadorLinks links, EscavadorPaginator paginator) {
}

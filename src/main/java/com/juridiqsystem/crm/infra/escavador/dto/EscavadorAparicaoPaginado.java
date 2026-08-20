package com.juridiqsystem.crm.infra.escavador.dto;

import java.util.List;

/** Envelope items/links/paginator (ver docs §Pagination) para {@code GET .../aparicoes}. */
public record EscavadorAparicaoPaginado(List<EscavadorAparicao> items, EscavadorLinks links, EscavadorPaginator paginator) {
}

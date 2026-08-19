package com.juridiqsystem.crm.infra.escavador.dto;

import java.util.List;

/** Envelope items/links/paginator (ver docs §Pagination) para endpoints que retornam lista de Processo. */
public record EscavadorProcessoPaginado(List<EscavadorProcesso> items, EscavadorLinks links, EscavadorPaginator paginator) {
}

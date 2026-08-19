package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Objeto {@code links} de um item de documento (não confundir com {@link EscavadorLinks}, o envelope de paginação). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorDocumentoLinks(String api) {
}

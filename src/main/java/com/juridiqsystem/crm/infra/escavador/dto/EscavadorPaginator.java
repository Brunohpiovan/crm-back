package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EscavadorPaginator(
        @JsonProperty("current_page") Integer currentPage,
        @JsonProperty("per_page") Integer perPage,
        Integer total,
        @JsonProperty("total_pages") Integer totalPages
) {
}

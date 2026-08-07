package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Envelope padrão de resposta paginada — nunca serializar Page/PageImpl do Spring Data diretamente (ver CONVENTIONS.md §6).")
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> conteudoCompleto, int page, int size) {
        long totalElements = conteudoCompleto.size();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        int from = Math.min(page * size, conteudoCompleto.size());
        int to = Math.min(from + size, conteudoCompleto.size());
        List<T> pagina = conteudoCompleto.subList(from, to);

        return new PageResponse<>(pagina, page, size, totalElements, totalPages);
    }
}

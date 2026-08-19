package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.ProcessoDocumento;
import com.juridiqsystem.crm.model.enums.FonteDocumento;

import java.time.LocalDate;

public record ProcessoDocumentoResponse(
        Long id,
        String titulo,
        String descricao,
        LocalDate dataDocumento,
        String tipo,
        String extensaoArquivo,
        Integer quantidadePaginas,
        FonteDocumento fonte
) {

    public ProcessoDocumentoResponse(ProcessoDocumento documento) {
        this(
                documento.getId(),
                documento.getTitulo(),
                documento.getDescricao(),
                documento.getDataDocumento(),
                documento.getTipo(),
                documento.getExtensaoArquivo(),
                documento.getQuantidadePaginas(),
                documento.getFonte()
        );
    }
}

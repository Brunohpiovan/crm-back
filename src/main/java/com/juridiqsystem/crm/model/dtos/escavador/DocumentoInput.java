package com.juridiqsystem.crm.model.dtos.escavador;

import com.juridiqsystem.crm.model.enums.FonteDocumento;

import java.time.LocalDate;

/** Entrada de ProcessoDocumentoService.registrarDocumentos — contrato compartilhado entre busca manual e callback, mesmo espírito de MovimentacaoInput. */
public record DocumentoInput(
        String escavadorDocumentoId,
        String titulo,
        String descricao,
        LocalDate dataDocumento,
        String tipo,
        String extensaoArquivo,
        Integer quantidadePaginas,
        FonteDocumento fonte) {
}

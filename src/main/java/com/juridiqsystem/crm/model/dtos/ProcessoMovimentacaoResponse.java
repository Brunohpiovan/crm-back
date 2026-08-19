package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.ProcessoMovimentacao;
import com.juridiqsystem.crm.model.enums.FonteMovimentacao;

import java.time.LocalDateTime;

public record ProcessoMovimentacaoResponse(Long id, LocalDateTime dataMovimentacao, String tipo, String conteudo, FonteMovimentacao fonte) {

    public ProcessoMovimentacaoResponse(ProcessoMovimentacao movimentacao) {
        this(movimentacao.getId(), movimentacao.getDataMovimentacao(), movimentacao.getTipo(),
                movimentacao.getConteudo(), movimentacao.getFonte());
    }
}

package com.juridiqsystem.crm.model.dtos.escavador;

import com.juridiqsystem.crm.model.enums.FonteMovimentacao;

import java.time.LocalDateTime;

/** Entrada de ProcessoMovimentacaoService.registrarMovimentacoes — ver contrato compartilhado com o Prompt 2. */
public record MovimentacaoInput(LocalDateTime dataMovimentacao, String tipo, String conteudo, FonteMovimentacao fonte) {
}

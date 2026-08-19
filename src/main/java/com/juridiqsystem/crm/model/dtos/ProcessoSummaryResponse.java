package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.enums.ProcessoSituacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProcessoSummaryResponse(
        String publicId,
        String numeroCnj,
        String tribunal,
        ProcessoSituacao situacao,
        BigDecimal valorCausa,
        LocalDateTime ultimaConsultaEm
) {

    public ProcessoSummaryResponse(Processo processo) {
        this(processo.getPublicId(), processo.getNumeroCnj(), processo.getTribunal(),
                processo.getSituacao(), processo.getValorCausa(), processo.getUltimaConsultaEm());
    }
}

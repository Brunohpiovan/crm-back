package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.ProcessoEnvolvido;
import com.juridiqsystem.crm.model.ProcessoMovimentacao;
import com.juridiqsystem.crm.model.enums.ProcessoSituacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProcessoDetailResponse(
        String publicId,
        String numeroCnj,
        String tribunal,
        String instancia,
        ProcessoSituacao situacao,
        BigDecimal valorCausa,
        LocalDate dataDistribuicao,
        String area,
        String assunto,
        String resumoIa,
        LocalDateTime resumoIaGeradoEm,
        LocalDateTime ultimaConsultaEm,
        List<ProcessoEnvolvidoResponse> envolvidos,
        List<ProcessoMovimentacaoResponse> movimentacoes
) {

    public ProcessoDetailResponse(Processo processo, List<ProcessoEnvolvido> envolvidos, List<ProcessoMovimentacao> movimentacoes) {
        this(
                processo.getPublicId(),
                processo.getNumeroCnj(),
                processo.getTribunal(),
                processo.getInstancia(),
                processo.getSituacao(),
                processo.getValorCausa(),
                processo.getDataDistribuicao(),
                processo.getArea(),
                processo.getAssunto(),
                processo.getResumoIa(),
                processo.getResumoIaGeradoEm(),
                processo.getUltimaConsultaEm(),
                envolvidos.stream().map(ProcessoEnvolvidoResponse::new).toList(),
                movimentacoes.stream().map(ProcessoMovimentacaoResponse::new).toList()
        );
    }
}

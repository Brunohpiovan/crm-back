package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Intimacao;
import com.juridiqsystem.crm.model.IntimacaoMonitoramento;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.enums.Uf;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Uma publicação em Diário Oficial encontrada para uma OAB monitorada, como exibida na página Intimações.")
public record IntimacaoResponse(

        Long id,

        Long intimacaoMonitoramentoId,

        String oabNumero,

        Uf oabUf,

        @Schema(description = "publicId do processo identificado pela Escavador. Null quando a publicação não pôde ser associada a um processo (evento diario_citacao_nova) — esse é o caso que mais precisa de atenção humana.")
        String processoPublicId,

        String numeroCnjIdentificado,

        String diarioNome,

        String diarioSigla,

        LocalDate diarioData,

        String conteudo,

        String link,

        boolean lida,

        LocalDateTime lidaEm,

        LocalDateTime criadoEm) {

    public static IntimacaoResponse from(Intimacao intimacao) {
        IntimacaoMonitoramento monitoramento = intimacao.getIntimacaoMonitoramento();
        Processo processo = intimacao.getProcesso();
        return new IntimacaoResponse(
                intimacao.getId(),
                monitoramento.getId(),
                monitoramento.getOabNumero(),
                monitoramento.getOabUf(),
                processo != null ? processo.getPublicId() : null,
                intimacao.getNumeroCnjIdentificado(),
                intimacao.getDiarioNome(),
                intimacao.getDiarioSigla(),
                intimacao.getDiarioData(),
                intimacao.getConteudo(),
                intimacao.getLink(),
                intimacao.getLidaEm() != null,
                intimacao.getLidaEm(),
                intimacao.getCriadoEm());
    }
}

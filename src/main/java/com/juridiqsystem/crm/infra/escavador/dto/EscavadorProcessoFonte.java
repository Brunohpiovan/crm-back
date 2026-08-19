package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EscavadorProcessoFonte(
        Long id,
        @JsonProperty("processo_fonte_id") Long processoFonteId,
        String descricao,
        String nome,
        String sigla,
        String tipo,
        @JsonProperty("data_inicio") String dataInicio,
        @JsonProperty("data_ultima_movimentacao") String dataUltimaMovimentacao,
        @JsonProperty("segredo_justica") Boolean segredoJustica,
        Boolean arquivado,
        @JsonProperty("status_predito") String statusPredito,
        Integer grau,
        @JsonProperty("grau_formatado") String grauFormatado,
        Boolean fisico,
        String sistema,
        String url,
        @JsonProperty("quantidade_movimentacoes") Integer quantidadeMovimentacoes,
        @JsonProperty("data_ultima_verificacao") String dataUltimaVerificacao,
        @JsonProperty("quantidade_envolvidos") Integer quantidadeEnvolvidos,
        List<EscavadorEnvolvido> envolvidos,
        EscavadorProcessoFonteCapa capa,
        EscavadorTribunal tribunal
) {
}

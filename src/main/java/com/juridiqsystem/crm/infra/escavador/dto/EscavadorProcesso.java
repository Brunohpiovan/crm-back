package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Corpo de "GET /api/v2/processos/numero_cnj/{numero}" — ver docs/integrations/escavador-api.md §8.4/§8.10. */
public record EscavadorProcesso(
        @JsonProperty("numero_cnj") String numeroCnj,
        @JsonProperty("titulo_polo_ativo") String tituloPoloAtivo,
        @JsonProperty("titulo_polo_passivo") String tituloPoloPassivo,
        @JsonProperty("ano_inicio") Integer anoInicio,
        @JsonProperty("data_inicio") String dataInicio,
        @JsonProperty("estado_origem") EscavadorEstado estadoOrigem,
        @JsonProperty("unidade_origem") EscavadorUnidadeOrigem unidadeOrigem,
        @JsonProperty("data_ultima_movimentacao") String dataUltimaMovimentacao,
        @JsonProperty("quantidade_movimentacoes") Integer quantidadeMovimentacoes,
        @JsonProperty("data_ultima_verificacao") String dataUltimaVerificacao,
        List<EscavadorProcessoFonte> fontes
) {
}

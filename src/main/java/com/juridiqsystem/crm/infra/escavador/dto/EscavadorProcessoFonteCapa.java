package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EscavadorProcessoFonteCapa(
        String classe,
        String assunto,
        @JsonProperty("assuntos_normalizados") List<EscavadorAssunto> assuntosNormalizados,
        @JsonProperty("assunto_principal_normalizado") EscavadorAssunto assuntoPrincipalNormalizado,
        String area,
        String situacao,
        @JsonProperty("orgao_julgador") String orgaoJulgador,
        @JsonProperty("orgao_julgador_normatizado") EscavadorUnidadeOrigem orgaoJulgadorNormatizado,
        @JsonProperty("valor_causa") EscavadorValorCausa valorCausa,
        @JsonProperty("data_distribuicao") String dataDistribuicao,
        @JsonProperty("data_arquivamento") String dataArquivamento,
        @JsonProperty("informacoes_complementares") List<EscavadorInformacaoComplementar> informacoesComplementares
) {
}

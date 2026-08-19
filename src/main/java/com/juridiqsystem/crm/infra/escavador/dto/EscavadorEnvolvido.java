package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EscavadorEnvolvido(
        String nome,
        @JsonProperty("quantidade_processos") Integer quantidadeProcessos,
        @JsonProperty("tipo_pessoa") String tipoPessoa,
        String prefixo,
        String sufixo,
        String tipo,
        @JsonProperty("tipo_normalizado") String tipoNormalizado,
        String polo,
        String cpf,
        String cnpj,
        List<EscavadorOab> oabs,
        List<EscavadorEnvolvido> advogados
) {
}

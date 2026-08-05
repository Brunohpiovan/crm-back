package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Estatísticas de uma etapa do funil de vendas no período filtrado.")
public record DashboardFunilEtapaResponse(
        @Schema(description = "Id da etapa") Long etapaId,
        @Schema(description = "Nome da etapa") String nome,
        @Schema(description = "Posição da etapa dentro do funil (1-based), pela ordem de criação — Etapa não possui campo de ordenação próprio no schema atual") Integer ordem,
        @Schema(description = "Quantidade de oportunidades na etapa (excluindo situação LIXEIRA)") Long quantidade,
        @Schema(description = "Soma do valor das oportunidades na etapa") BigDecimal valorTotal,
        @Schema(description = "Percentual do valorTotal desta etapa em relação à soma de todas as etapas retornadas") Double percentual
) {
}

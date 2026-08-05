package br.edu.faculdadevincit.crm_vincit.model.dtos;

import java.math.BigDecimal;

public record DashboardRankingOportunidadeRow(
        Long usuarioId,
        String usuarioNome,
        Long oportunidadesGanhas,
        Long oportunidadesPerdidas,
        BigDecimal valorVendido
) {
}

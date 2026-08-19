package com.juridiqsystem.crm.model.dtos;

import java.math.BigDecimal;

public record DashboardRankingOportunidadeRow(
        Long usuarioId,
        String usuarioNome,
        Long oportunidadesGanhas,
        Long oportunidadesPerdidas,
        BigDecimal valorVendido
) {
}

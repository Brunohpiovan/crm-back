package com.juridiqsystem.crm.model.dtos;

public record DashboardRankingProtocoloRow(
        Long usuarioId,
        String usuarioNome,
        Long protocolosFechados,
        Double tempoMedioAtendimentoMinutos
) {
}

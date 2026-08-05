package br.edu.faculdadevincit.crm_vincit.model.dtos;

public record DashboardRankingProtocoloRow(
        Long usuarioId,
        String usuarioNome,
        Long protocolosFechados,
        Double tempoMedioAtendimentoMinutos
) {
}

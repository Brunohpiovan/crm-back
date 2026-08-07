package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Empresa;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "Dados de uma empresa (tenant), retornados pelos endpoints /master/empresas.")
@Getter
public class EmpresaResponseDTO {

    private final Long id;
    private final String codigo;
    private final String nome;
    private final String logoUrl;
    private final String timezone;
    private final Integer protocoloRiscoHoras;
    private final Boolean notificacaoVisualHabilitada;
    private final Boolean notificacaoSonoraHabilitada;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;

    public EmpresaResponseDTO(Empresa empresa) {
        this.id = empresa.getId();
        this.codigo = empresa.getCodigo();
        this.nome = empresa.getNome();
        this.logoUrl = empresa.getLogoUrl();
        this.timezone = empresa.getTimezone();
        this.protocoloRiscoHoras = empresa.getProtocoloRiscoHoras();
        this.notificacaoVisualHabilitada = empresa.getNotificacaoVisualHabilitada();
        this.notificacaoSonoraHabilitada = empresa.getNotificacaoSonoraHabilitada();
        this.criadoEm = empresa.getCriadoEm();
        this.atualizadoEm = empresa.getAtualizadoEm();
    }
}

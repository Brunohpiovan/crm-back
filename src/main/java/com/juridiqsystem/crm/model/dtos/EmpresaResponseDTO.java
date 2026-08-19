package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Empresa;
import com.juridiqsystem.crm.model.enums.FusoHorarioEmpresa;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "Dados de uma empresa (tenant), retornados pelos endpoints /master/empresas.")
@Getter
public class EmpresaResponseDTO {

    private final String id;
    private final String codigo;
    private final String nome;
    private final String logoUrl;
    private final FusoHorarioEmpresa timezone;
    private final Integer protocoloRiscoHoras;
    private final Boolean notificacaoVisualHabilitada;
    private final Boolean notificacaoSonoraHabilitada;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;

    @Schema(description = "Quantidade de usuários ativos (não bloqueados) da empresa. Só é preenchido pela listagem paginada (GET /master/empresas); nos demais endpoints (busca por id, criação, edição, autoatendimento) vem null, pois calcular isso exigiria uma query extra sem uso na tela de detalhe/edição.")
    private final Long quantidadeUsuarios;

    @Schema(description = "Quantidade de usuários ativos com cargo ADMINISTRADOR na empresa. Mesma ressalva de quantidadeUsuarios: só vem preenchido na listagem paginada.")
    private final Long quantidadeAdmins;

    public EmpresaResponseDTO(Empresa empresa) {
        this(empresa, null, null);
    }

    public EmpresaResponseDTO(Empresa empresa, Long quantidadeUsuarios, Long quantidadeAdmins) {
        this.id = empresa.getPublicId();
        this.codigo = empresa.getCodigo();
        this.nome = empresa.getNome();
        this.logoUrl = empresa.getLogoUrl();
        this.timezone = empresa.getTimezone();
        this.protocoloRiscoHoras = empresa.getProtocoloRiscoHoras();
        this.notificacaoVisualHabilitada = empresa.getNotificacaoVisualHabilitada();
        this.notificacaoSonoraHabilitada = empresa.getNotificacaoSonoraHabilitada();
        this.criadoEm = empresa.getCriadoEm();
        this.atualizadoEm = empresa.getAtualizadoEm();
        this.quantidadeUsuarios = quantidadeUsuarios;
        this.quantidadeAdmins = quantidadeAdmins;
    }
}

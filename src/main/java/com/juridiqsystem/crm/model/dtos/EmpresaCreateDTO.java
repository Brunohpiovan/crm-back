package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Payload de criação/edição de uma empresa (tenant), usado pelo usuário master em /master/empresas.")
@Getter
@Setter
@NoArgsConstructor
public class EmpresaCreateDTO {

    @Schema(description = "Código curto e único usado no login (codigoEmpresa)")
    @NotBlank(message = "Informe um código para a empresa")
    @Size(max = 60, message = "O código deve ter no máximo 60 caracteres")
    private String codigo;

    @NotBlank(message = "Informe um nome para a empresa")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nome;

    private String logoUrl;

    @NotBlank(message = "Informe o timezone da empresa")
    @Size(max = 60, message = "O timezone deve ter no máximo 60 caracteres")
    private String timezone;

    @NotNull(message = "Informe o protocoloRiscoHoras")
    private Integer protocoloRiscoHoras;

    @NotNull(message = "Informe notificacaoVisualHabilitada")
    private Boolean notificacaoVisualHabilitada;

    @NotNull(message = "Informe notificacaoSonoraHabilitada")
    private Boolean notificacaoSonoraHabilitada;
}

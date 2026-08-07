package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.enums.FusoHorarioEmpresa;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Payload de autoatendimento para a empresa do usuário autenticado editar os próprios dados, em PUT /empresa. Ao contrário de EmpresaCreateDTO (usado pelo master em /master/empresas), não inclui `codigo` (login da empresa) nem `interna`.")
@Getter
@Setter
@NoArgsConstructor
public class EmpresaSelfUpdateDTO {

    @NotBlank(message = "Informe um nome para a empresa")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nome;

    @Schema(description = "URL atual do logo, para o caso de nenhum arquivo novo ser enviado (ex.: usuário só mudou outro campo) ou de o logo ter sido removido (null/vazio).")
    private String logoUrl;

    @NotNull(message = "Informe o timezone da empresa")
    private FusoHorarioEmpresa timezone;

    @NotNull(message = "Informe o protocoloRiscoHoras")
    private Integer protocoloRiscoHoras;

    @NotNull(message = "Informe notificacaoVisualHabilitada")
    private Boolean notificacaoVisualHabilitada;

    @NotNull(message = "Informe notificacaoSonoraHabilitada")
    private Boolean notificacaoSonoraHabilitada;
}

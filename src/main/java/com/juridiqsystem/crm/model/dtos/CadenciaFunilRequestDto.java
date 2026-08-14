package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.enums.Situacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Schema(description = "Corpo de criação/atualização de uma cadência de funil (POST /cadencia e PUT /cadencia/{id}).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CadenciaFunilRequestDto {

    @NotBlank(message = "Informe um nome")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nome;
    @Schema(description = "Id do funil de onde as oportunidades elegíveis serão movidas")
    @NotBlank(message = "Informe o funil de origem")
    private String funilOrigem;
    @Schema(description = "Id da etapa de onde as oportunidades elegíveis serão movidas")
    @NotBlank(message = "Informe a etapa de origem")
    private String etapaOrigem;
    @Schema(description = "Id do funil para onde as oportunidades serão movidas")
    @NotBlank(message = "Informe o funil de destino")
    private String funilDestino;
    @Schema(description = "Id da etapa para onde as oportunidades serão movidas")
    @NotBlank(message = "Informe a etapa de destino")
    private String etapaDestino;
    @Schema(description = "Quantidade de dias que uma oportunidade precisa permanecer na etapa de origem para se tornar elegível para a movimentação automática")
    @NotNull(message = "Informe a quantidade de dias na etapa")
    @Positive(message = "A quantidade de dias na etapa deve ser maior que zero")
    @Max(value = 3650, message = "A quantidade de dias na etapa deve ser no máximo 3650")
    private Integer diasNaEtapa;
    @Schema(description = "Horário do dia em que o scheduler deve executar a movimentação das oportunidades elegíveis")
    @NotNull(message = "Informe o horário de movimentação")
    private LocalTime horarioMovimentacao;
    @Schema(description = "Situação da cadência: apenas cadências ATIVA são processadas pelo scheduler de movimentação automática")
    @NotNull(message = "Informe a situação")
    private Situacao situacao;
    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    private String descricao;
}

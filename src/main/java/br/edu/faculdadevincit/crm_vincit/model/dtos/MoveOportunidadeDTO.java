package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Corpo de PUT /oportunidade/move: para onde e em qual posição uma oportunidade deve ser movida no Kanban.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoveOportunidadeDTO {
    @Schema(description = "Id da oportunidade a ser movida")
    @NotNull(message = "Informe a oportunidade")
    private String oportunidadeId;
    @Schema(description = "Id da etapa de destino (pode ser a mesma etapa atual, para apenas reordenar)")
    @NotNull(message = "Informe a etapa")
    private String etapaId;
    @Schema(description = "Posição (0-based) que a oportunidade deve ocupar na lista de oportunidades da etapa de destino")
    @Min(value = 0, message = "O indice deve ser maior ou igual a zero")
    private int indice;
}

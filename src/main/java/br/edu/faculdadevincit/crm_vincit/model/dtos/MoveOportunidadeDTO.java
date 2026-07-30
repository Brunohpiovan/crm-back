package br.edu.faculdadevincit.crm_vincit.model.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoveOportunidadeDTO {
    @NotNull(message = "Informe a oportunidade")
    private Long oportunidadeId;
    @NotNull(message = "Informe a etapa")
    private Long etapaId;
    @Min(value = 0, message = "O indice deve ser maior ou igual a zero")
    private int indice;
}

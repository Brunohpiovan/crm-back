package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import io.swagger.v3.oas.annotations.media.Schema;
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

    private String nome;
    @Schema(description = "Id do funil de onde as oportunidades elegíveis serão movidas")
    private String funilOrigem;
    @Schema(description = "Id da etapa de onde as oportunidades elegíveis serão movidas")
    private String etapaOrigem;
    @Schema(description = "Id do funil para onde as oportunidades serão movidas")
    private String funilDestino;
    @Schema(description = "Id da etapa para onde as oportunidades serão movidas")
    private String etapaDestino;
    @Schema(description = "Quantidade de dias que uma oportunidade precisa permanecer na etapa de origem para se tornar elegível para a movimentação automática")
    private Integer diasNaEtapa;
    @Schema(description = "Horário do dia em que o scheduler deve executar a movimentação das oportunidades elegíveis")
    private LocalTime horarioMovimentacao;
    @Schema(description = "Situação da cadência: apenas cadências ATIVA são processadas pelo scheduler de movimentação automática")
    private Situacao situacao;
    private String descricao;
}

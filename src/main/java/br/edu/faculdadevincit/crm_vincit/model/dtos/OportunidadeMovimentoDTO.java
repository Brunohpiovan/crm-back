package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Payload publicado em /topic/movimentoOportunidade: apenas os ids necessários para o cliente reposicionar o card localmente, sem reenviar a oportunidade inteira.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OportunidadeMovimentoDTO {

    @Schema(description = "Id da oportunidade que foi movida")
    private Long oportunidadeId;

    @Schema(description = "Id da etapa de destino para onde a oportunidade foi movida")
    private Long etapaId;

}

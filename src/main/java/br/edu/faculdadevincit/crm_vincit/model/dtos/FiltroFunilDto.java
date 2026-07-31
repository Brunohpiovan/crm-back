package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "Critérios de filtro usados em POST /funil/filtro para buscar um funil e restringir as oportunidades exibidas em cada etapa.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FiltroFunilDto {

    @Schema(description = "Id do funil a ser buscado")
    private Long id;
    @Schema(description = "Lista de situações de oportunidade a incluir no resultado (ex.: ABERTO, GANHO, PERDIDO). Se vazia/nula, não filtra por situação.")
    private List<SituacaoOportunidade> situacoes;
    @Schema(description = "Lista de tags a incluir no resultado (apenas oportunidades com pelo menos uma das tags informadas). Se vazia/nula, não filtra por tag.")
    private List<TagOportunidadeDTO> tags;
}

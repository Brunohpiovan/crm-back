package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FiltroFunilDto {

    private Long id;
    private List<SituacaoOportunidade> situacoes;
    private List<TagOportunidadeDTO> tags;
}

package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Tag;
import br.edu.faculdadevincit.crm_vincit.model.enums.Cor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Representação resumida de uma tag (id, nome e cor), usada em GET /tag/ativas e aninhada dentro de OportunidadeDTO/FiltroFunilDto.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TagOportunidadeDTO {

    private Long id;
    private String nome;
    @Schema(description = "Cor de exibição da tag na interface")
    private Cor cor;

    public TagOportunidadeDTO(Tag tag){
        this.id = tag.getId();
        this.nome = tag.getNome();
        this.cor = tag.getCor();
    }
}

package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Tag;
import br.edu.faculdadevincit.crm_vincit.model.enums.Cor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TagOportunidadeDTO {

    private Long id;
    private String nome;
    private Cor cor;

    public TagOportunidadeDTO(Tag tag){
        this.id = tag.getId();
        this.nome = tag.getNome();
        this.cor = tag.getCor();
    }
}

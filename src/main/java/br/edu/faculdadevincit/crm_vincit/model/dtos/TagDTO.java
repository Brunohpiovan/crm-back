package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Tag;
import br.edu.faculdadevincit.crm_vincit.model.TemplateEmail;
import br.edu.faculdadevincit.crm_vincit.model.enums.Cor;
import br.edu.faculdadevincit.crm_vincit.model.enums.Pertence;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TagDTO {

    private Long id;

    private String nome;

    private Cor cor;

    private Pertence pertence;

    private Situacao situacao;


    public TagDTO(Tag tag){
        this.id = tag.getId();
        this.nome = tag.getNome();
        this.cor = tag.getCor();
        this.pertence = tag.getPertence();
        this.situacao = tag.getSituacao();
    }

}

package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Funil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunilDtoCard {
    private Long id;
    private String nome;

    public FunilDtoCard(Funil funil) {
        this.id = funil.getId();
        this.nome = funil.getNome();
    }
}

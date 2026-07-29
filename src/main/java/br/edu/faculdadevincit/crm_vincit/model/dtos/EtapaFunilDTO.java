package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapaFunilDTO {

    private Long id;
    private String nome;

    public EtapaFunilDTO(Etapa etapa) {
        this.id = etapa.getId();
        this.nome = etapa.getNome();

    }
}

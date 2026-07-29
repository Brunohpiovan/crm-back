package br.edu.faculdadevincit.crm_vincit.model.dtos;


import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapaUpdate2DTO {
    private Long id;
    private String nome;

    public EtapaUpdate2DTO(Etapa etapa) {
        this.id = etapa.getId();
        this.nome = etapa.getNome();
    }
}

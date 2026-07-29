package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Funil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunilDto {

    private Long id;
    private String nome;
    private List<EtapaDto> etapas;

    public FunilDto(Funil funil) {
        this.id = funil.getId();
        this.nome = funil.getNome();
        this.etapas = new ArrayList<>();

        for (Etapa etapa : funil.getEtapas()) {
            this.etapas.add(new EtapaDto(etapa));
        }
    }
}



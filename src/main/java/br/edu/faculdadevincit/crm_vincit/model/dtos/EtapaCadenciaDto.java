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
public class EtapaCadenciaDto {

    private Long id;
    private String nome;

    public EtapaCadenciaDto(Etapa etapaOrigem) {
        this.id = etapaOrigem.getId();
        this.nome = etapaOrigem.getNome();
    }
}

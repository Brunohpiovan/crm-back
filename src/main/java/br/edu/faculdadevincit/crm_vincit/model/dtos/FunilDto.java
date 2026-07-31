package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Funil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Funil completo, com todas as suas etapas e, dentro de cada etapa, as oportunidades correspondentes (possivelmente já filtradas por situação/tags).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunilDto {

    private Long id;
    private String nome;
    @Schema(description = "Etapas do funil, cada uma com suas oportunidades")
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



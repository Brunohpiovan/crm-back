package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapaDto {
    private Long id;
    private String nome;
    private String posicao;
    private List<OportunidadeDTO> oportunidades;
    private FunilDtoCard funil;
    private BigDecimal valor_total;

    public EtapaDto(Etapa etapa) {
        this.id = etapa.getId();
        this.nome = etapa.getNome();
        this.funil = new FunilDtoCard(etapa.getFunil());
        //this.posicao = card.getPosicao();
        this.oportunidades = new ArrayList<>();
        for (Oportunidade oportunidade : etapa.getOportunidades()) {
            this.oportunidades.add(new OportunidadeDTO(oportunidade));
        }
        this.valor_total = etapa.getValor_total();
    }

    public EtapaDto(Etapa etapa, List<OportunidadeDTO> oportunidades) {
        this.id = etapa.getId();
        this.nome = etapa.getNome();
        this.funil = new FunilDtoCard(etapa.getFunil());
        this.oportunidades = oportunidades != null ? oportunidades : new ArrayList<>();
        this.valor_total = etapa.getValor_total();
    }
}

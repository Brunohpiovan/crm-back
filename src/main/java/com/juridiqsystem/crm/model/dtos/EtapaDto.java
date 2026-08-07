package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Etapa;
import com.juridiqsystem.crm.model.Oportunidade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Etapa (coluna) de um funil, com as oportunidades nela contidas (possivelmente já filtradas por situação/tags) e o valor total acumulado.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapaDto {
    private String id;
    private String nome;
    @Schema(description = "Posição da etapa dentro do funil (ordem de exibição das colunas no board)")
    private Integer posicao;
    @Schema(description = "Oportunidades pertencentes a esta etapa, na ordem definida pelo índice de cada uma")
    private List<OportunidadeDTO> oportunidades;
    private FunilDtoCard funil;
    @Schema(description = "Soma do valor de todas as oportunidades atualmente nesta etapa")
    private BigDecimal valor_total;

    public EtapaDto(Etapa etapa) {
        this.id = etapa.getPublicId();
        this.nome = etapa.getNome();
        this.funil = new FunilDtoCard(etapa.getFunil());
        this.posicao = etapa.getPosicao();
        this.oportunidades = new ArrayList<>();
        for (Oportunidade oportunidade : etapa.getOportunidades()) {
            this.oportunidades.add(new OportunidadeDTO(oportunidade));
        }
        this.valor_total = etapa.getValor_total();
    }

    public EtapaDto(Etapa etapa, List<OportunidadeDTO> oportunidades) {
        this.id = etapa.getPublicId();
        this.nome = etapa.getNome();
        this.funil = new FunilDtoCard(etapa.getFunil());
        this.posicao = etapa.getPosicao();
        this.oportunidades = oportunidades != null ? oportunidades : new ArrayList<>();
        this.valor_total = etapa.getValor_total();
    }
}

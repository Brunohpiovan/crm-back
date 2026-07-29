package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.CadenciaFunil;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CadenciaAllDTO {
    private Long id;
    private String nome;
    private String funilOrigem;
    private String etapaOrigem;
    private String funilDestino;
    private String etapaDestino;
    private Integer diasNaEtapa;
    private LocalTime horarioMovimentacao;
    private Situacao situacao;

    public CadenciaAllDTO(CadenciaFunil cadenciaFunil) {
        this.id = cadenciaFunil.getId();
        this.nome = cadenciaFunil.getNome();
        this.funilOrigem = cadenciaFunil.getFunilOrigem().getNome();
        this.etapaOrigem = cadenciaFunil.getEtapaOrigem().getNome();
        this.funilDestino = cadenciaFunil.getFunilDestino().getNome();
        this.etapaDestino = cadenciaFunil.getEtapaDestino().getNome();
        this.diasNaEtapa = cadenciaFunil.getDiasNaEtapa();
        this.horarioMovimentacao = cadenciaFunil.getHorarioMovimentacao();
        this.situacao = cadenciaFunil.getSituacao();
    }
}

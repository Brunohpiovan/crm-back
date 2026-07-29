package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.CadenciaFunil;
import br.edu.faculdadevincit.crm_vincit.model.Etapa;
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
public class CadenciaFunilDto {

    private Long id;
    private String nome;
    private FunilAllDTO funilOrigem;
    private EtapaCadenciaDto etapaOrigem;
    private FunilAllDTO funilDestino;
    private EtapaCadenciaDto etapaDestino;
    private Integer diasNaEtapa;
    private LocalTime horarioMovimentacao;
    private Situacao situacao;
    private String descricao;

    public CadenciaFunilDto(CadenciaFunil cadenciaFunil) {
        this.id = cadenciaFunil.getId();
        this.nome = cadenciaFunil.getNome();
        this.funilOrigem = new FunilAllDTO(cadenciaFunil.getFunilOrigem());
        this.etapaOrigem = new EtapaCadenciaDto(cadenciaFunil.getEtapaOrigem());
        this.funilDestino = new FunilAllDTO(cadenciaFunil.getFunilDestino());
        this.etapaDestino = new EtapaCadenciaDto(cadenciaFunil.getEtapaDestino());
        this.diasNaEtapa = cadenciaFunil.getDiasNaEtapa();
        this.horarioMovimentacao = cadenciaFunil.getHorarioMovimentacao();
        this.situacao = cadenciaFunil.getSituacao();
        this.descricao = cadenciaFunil.getDescricao();
    }
}

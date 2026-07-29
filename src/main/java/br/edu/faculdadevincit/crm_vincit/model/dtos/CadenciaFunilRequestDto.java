package br.edu.faculdadevincit.crm_vincit.model.dtos;

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
public class CadenciaFunilRequestDto {

    private String nome;
    private Long funilOrigem;
    private Long etapaOrigem;
    private Long funilDestino;
    private Long etapaDestino;
    private Integer diasNaEtapa;
    private LocalTime horarioMovimentacao;
    private Situacao situacao;
    private String descricao;
}

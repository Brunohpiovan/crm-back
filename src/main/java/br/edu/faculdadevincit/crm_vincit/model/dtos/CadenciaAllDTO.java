package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.CadenciaFunil;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
@Schema(description = "Representação resumida de uma cadência de funil para listagem: nomes (já resolvidos) do funil/etapa de origem e destino, ao invés dos ids.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CadenciaAllDTO {
    private String id;
    private String nome;
    @Schema(description = "Nome do funil de origem, de onde as oportunidades elegíveis são movidas")
    private String funilOrigem;
    @Schema(description = "Nome da etapa de origem, de onde as oportunidades elegíveis são movidas")
    private String etapaOrigem;
    @Schema(description = "Nome do funil de destino, para onde as oportunidades são movidas")
    private String funilDestino;
    @Schema(description = "Nome da etapa de destino, para onde as oportunidades são movidas")
    private String etapaDestino;
    @Schema(description = "Quantidade de dias que uma oportunidade precisa permanecer na etapa de origem para se tornar elegível para a movimentação automática")
    private Integer diasNaEtapa;
    @Schema(description = "Horário do dia em que o scheduler executa a movimentação das oportunidades elegíveis")
    private LocalTime horarioMovimentacao;
    @Schema(description = "Situação da cadência: apenas cadências ATIVA são processadas pelo scheduler de movimentação automática")
    private Situacao situacao;

    public CadenciaAllDTO(CadenciaFunil cadenciaFunil) {
        this.id = cadenciaFunil.getPublicId();
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

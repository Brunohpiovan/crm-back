package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.CadenciaFunil;
import com.juridiqsystem.crm.model.Etapa;
import com.juridiqsystem.crm.model.enums.Situacao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Schema(description = "Detalhes completos de uma cadência de funil, retornado por GET /cadencia/{id}, com o funil e a etapa de origem/destino já resolvidos (id + nome).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CadenciaFunilDto {

    private String id;
    private String nome;
    @Schema(description = "Funil de onde as oportunidades elegíveis são movidas")
    private FunilAllDTO funilOrigem;
    @Schema(description = "Etapa de onde as oportunidades elegíveis são movidas")
    private EtapaCadenciaDto etapaOrigem;
    @Schema(description = "Funil para onde as oportunidades são movidas")
    private FunilAllDTO funilDestino;
    @Schema(description = "Etapa para onde as oportunidades são movidas")
    private EtapaCadenciaDto etapaDestino;
    @Schema(description = "Quantidade de dias que uma oportunidade precisa permanecer na etapa de origem para se tornar elegível para a movimentação automática")
    private Integer diasNaEtapa;
    @Schema(description = "Horário do dia em que o scheduler executa a movimentação das oportunidades elegíveis")
    private LocalTime horarioMovimentacao;
    @Schema(description = "Situação da cadência: apenas cadências ATIVA são processadas pelo scheduler de movimentação automática")
    private Situacao situacao;
    private String descricao;

    public CadenciaFunilDto(CadenciaFunil cadenciaFunil) {
        this.id = cadenciaFunil.getPublicId();
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

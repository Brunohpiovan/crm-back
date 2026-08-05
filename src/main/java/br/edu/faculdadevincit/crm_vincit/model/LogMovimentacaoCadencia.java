package br.edu.faculdadevincit.crm_vincit.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "Auditoria de movimentação automática de oportunidade via cadência. Só uma linha por movimentação real do scheduler (nunca movimentação manual) — ver CadenciaFunilService.moverOportunidades.")
@Getter
@Setter
@Entity
@Table(name = "log_movimentacao_cadencia")
@NoArgsConstructor
@AllArgsConstructor
public class LogMovimentacaoCadencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cadencia_funil_id", nullable = false)
    private Long cadenciaFunilId;

    @Column(name = "oportunidade_id", nullable = false)
    private Long oportunidadeId;

    @Column(name = "etapa_origem_id", nullable = false)
    private Long etapaOrigemId;

    @Column(name = "etapa_destino_id", nullable = false)
    private Long etapaDestinoId;

    @Column(name = "executado_em", nullable = false)
    private LocalDateTime executadoEm;

    public LogMovimentacaoCadencia(Long cadenciaFunilId, Long oportunidadeId, Long etapaOrigemId, Long etapaDestinoId) {
        this.cadenciaFunilId = cadenciaFunilId;
        this.oportunidadeId = oportunidadeId;
        this.etapaOrigemId = etapaOrigemId;
        this.etapaDestinoId = etapaDestinoId;
        this.executadoEm = LocalDateTime.now();
    }
}

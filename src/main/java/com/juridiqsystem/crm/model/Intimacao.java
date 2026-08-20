package com.juridiqsystem.crm.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Publicação/aparição em Diário Oficial encontrada para uma OAB monitorada, obtida via callback
 * ({@code diario_movimentacao_nova} quando a Escavador identifica o processo, ou
 * {@code diario_citacao_nova} quando não identifica). Análogo a {@link ProcessoDocumento}:
 * {@code chaveDedupe} é a natural key usada para idempotência entre reentregas do mesmo callback
 * (até 11x) — ver IntimacaoService.registrarDoCallback.
 *
 * <p>Deliberadamente SEM @TenantId (mesmo racional de ProcessoDocumento): o tenant é resolvido
 * através de {@code intimacaoMonitoramento}, que tem @TenantId — qualquer query que faça join com
 * essa relação já herda o filtro automático de tenant do Hibernate.</p>
 */
@Schema(description = "Uma publicação em Diário Oficial encontrada para uma OAB monitorada.")
@Getter
@Setter
@Entity
@Table(name = "intimacao")
@NoArgsConstructor
public class Intimacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intimacao_monitoramento_id", nullable = false)
    private IntimacaoMonitoramento intimacaoMonitoramento;

    @Schema(description = "Processo identificado pela Escavador para esta publicação. Null quando o evento foi diario_citacao_nova (a Escavador não conseguiu associar um processo) — esse é justamente o caso que mais precisa de atenção humana, então NÃO é descartado.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id")
    private Processo processo;

    @Column(name = "numero_cnj_identificado", length = 25)
    private String numeroCnjIdentificado;

    @Column(name = "diario_nome")
    private String diarioNome;

    @Column(name = "diario_sigla", length = 20)
    private String diarioSigla;

    @Column(name = "diario_data")
    private LocalDate diarioData;

    @Column(name = "conteudo", columnDefinition = "TEXT")
    private String conteudo;

    @Column(name = "link", length = 500)
    private String link;

    @Schema(description = "Natural key a partir de monitoramento_id+diario_id+página, para sobreviver a reentrega de callback (até 11x).")
    @Column(name = "chave_dedupe", nullable = false, length = 255)
    private String chaveDedupe;

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public void marcarComoLida() {
        this.lidaEm = LocalDateTime.now();
    }
}

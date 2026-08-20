package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.Uf;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

/**
 * Assinatura de monitoramento contínuo de uma OAB nos Diários Oficiais (Escavador API v1). Mirror
 * ponto a ponto de {@link ProcessoMonitoramento}, mas por OAB em vez de por processo: existe no
 * máximo uma linha por (empresa, oabNumero, oabUf) — ver unique — e um escritório com N advogados
 * cadastra N linhas, cada uma consumindo uma vaga da cota do plano.
 */
@Schema(description = "Assinatura de monitoramento contínuo de uma OAB nos Diários Oficiais (Escavador). Existe no máximo uma linha por (empresa, oabNumero, oabUf): reativar uma OAB desligada atualiza a linha existente em vez de criar outra.")
@Getter
@Setter
@Entity
@Table(name = "intimacao_monitoramento")
@NoArgsConstructor
public class IntimacaoMonitoramento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "oab_numero", nullable = false, length = 20)
    private String oabNumero;

    @Enumerated(EnumType.STRING)
    @Column(name = "oab_uf", nullable = false, length = 2)
    private Uf oabUf;

    @Schema(description = "Advogado do escritório dono desta OAB — vínculo só informativo (filtro/exibição), nunca usado para autorização.")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuarioAdvogado;

    @Schema(description = "CSV dos ids de origem (diários) resolvidos a partir da UF da OAB, enviados como origem_ids na criação da assinatura na Escavador.")
    @Column(name = "origem_ids", columnDefinition = "TEXT")
    private String origemIds;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Schema(description = "Id da assinatura na Escavador, devolvido na criação. Fica null enquanto a criação não foi confirmada — é exatamente essa condição (ativo = true e id null) que o IntimacaoMonitoramentoScheduler reconcilia.")
    @Column(name = "escavador_monitoramento_id", length = 40)
    private String escavadorMonitoramentoId;

    @Column(name = "ativado_em", nullable = false)
    private LocalDateTime ativadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ativado_por")
    private Usuario ativadoPor;

    @Column(name = "desativado_em")
    private LocalDateTime desativadoEm;

    /**
     * Usado tanto na primeira ativação quanto na reativação da mesma linha (mesma OAB desligada e
     * ligada de novo). Zera o escavadorMonitoramentoId de propósito: a assinatura anterior já foi
     * removida na Escavador ao desativar, então o id antigo não vale mais.
     */
    public void ativar(String oabNumero, Uf oabUf, Usuario usuarioAdvogado, String origemIds, Usuario ativadoPor) {
        this.oabNumero = oabNumero;
        this.oabUf = oabUf;
        this.usuarioAdvogado = usuarioAdvogado;
        this.origemIds = origemIds;
        this.ativo = true;
        this.ativadoPor = ativadoPor;
        this.ativadoEm = LocalDateTime.now();
        this.desativadoEm = null;
        this.escavadorMonitoramentoId = null;
    }

    public void desativar() {
        this.ativo = false;
        this.desativadoEm = LocalDateTime.now();
        this.escavadorMonitoramentoId = null;
    }
}

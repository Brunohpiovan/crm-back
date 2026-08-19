package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.FrequenciaMonitoramento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
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

@Schema(description = "Assinatura de monitoramento contínuo de um processo na Escavador. Existe no máximo uma linha por processo: reativar um monitoramento desligado atualiza a linha existente em vez de criar outra (ver unique em processo_id).")
@Getter
@Setter
@Entity
@Table(name = "processo_monitoramento")
@NoArgsConstructor
public class ProcessoMonitoramento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequencia", nullable = false, length = 20)
    private FrequenciaMonitoramento frequencia;

    @Schema(description = "Se true, o monitoramento também busca documentos públicos do processo (documentos_publicos na API do Escavador) — custa mais por processo.")
    @Column(name = "com_documentos", nullable = false)
    private Boolean comDocumentos;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Schema(description = "Id da assinatura na Escavador, devolvido na criação. Fica null enquanto a criação não foi confirmada — é exatamente essa condição (ativo = true e id null) que o EscavadorMonitoramentoScheduler reconcilia.")
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
     * Usado tanto na primeira ativação quanto na reativação da mesma linha. Zera o
     * escavadorMonitoramentoId de propósito: a assinatura anterior já foi removida na Escavador
     * ao desativar, então o id antigo não vale mais e a reativação precisa criar outra.
     */
    public void ativar(FrequenciaMonitoramento frequencia, boolean comDocumentos, Usuario ativadoPor) {
        this.frequencia = frequencia;
        this.comDocumentos = comDocumentos;
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

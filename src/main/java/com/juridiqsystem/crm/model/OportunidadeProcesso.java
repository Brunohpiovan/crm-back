package com.juridiqsystem.crm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Vínculo N:N entre Oportunidade e Processo — excluir esta linha nunca apaga o Processo (ver OportunidadeProcessoService). */
@Getter
@Setter
@Entity
@Table(name = "oportunidade_processo")
@NoArgsConstructor
@AllArgsConstructor
public class OportunidadeProcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oportunidade_id", nullable = false)
    private Oportunidade oportunidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Column(name = "vinculado_em", nullable = false)
    private LocalDateTime vinculadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vinculado_por", nullable = false)
    private Usuario vinculadoPor;
}

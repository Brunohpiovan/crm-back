package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.FonteMovimentacao;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * hashConteudo (SHA-256 do texto normalizado) + (processo_id, data_movimentacao) formam a
 * constraint única que torna ProcessoMovimentacaoService.registrarMovimentacoes idempotente entre
 * consulta manual e callback de monitoramento (Prompt 2) — ver migration desta tabela.
 */
@Getter
@Setter
@Entity
@Table(name = "processo_movimentacao")
@NoArgsConstructor
@AllArgsConstructor
public class ProcessoMovimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Column(name = "data_movimentacao", nullable = false)
    private LocalDateTime dataMovimentacao;

    @Column(name = "tipo", length = 60)
    private String tipo;

    @Column(name = "conteudo", columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Column(name = "hash_conteudo", nullable = false, length = 64)
    private String hashConteudo;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonte", nullable = false, length = 30)
    private FonteMovimentacao fonte;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}

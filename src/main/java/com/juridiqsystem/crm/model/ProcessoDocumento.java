package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.FonteDocumento;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Documento público de um processo, obtido sob demanda (busca manual) ou via callback de
 * monitoramento ({@code novo_documento}). {@code escavadorDocumentoId} é o {@code key}/{@code id}
 * devolvido pela Escavador, usado tanto para dedupe (constraint única com processo_id — mesmo
 * callback pode chegar repetido, até 11x) quanto para o download do PDF sob demanda.
 */
@Getter
@Setter
@Entity
@Table(name = "processo_documento")
@NoArgsConstructor
@AllArgsConstructor
public class ProcessoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Column(name = "escavador_documento_id", nullable = false, length = 255)
    private String escavadorDocumentoId;

    @Column(name = "titulo", length = 255)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "data_documento")
    private LocalDate dataDocumento;

    @Column(name = "tipo", length = 60)
    private String tipo;

    @Column(name = "extensao_arquivo", length = 20)
    private String extensaoArquivo;

    @Column(name = "quantidade_paginas")
    private Integer quantidadePaginas;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonte", nullable = false, length = 30)
    private FonteDocumento fonte;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}

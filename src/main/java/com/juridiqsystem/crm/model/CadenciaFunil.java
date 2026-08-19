package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.Situacao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "cadencia_funil")
@AllArgsConstructor
@Entity
public class CadenciaFunil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "funil_origem_id", nullable = false)
    private Funil funilOrigem;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_origem_id", nullable = false)
    private Etapa etapaOrigem;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "funil_destino_id", nullable = false)
    private Funil funilDestino;


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_destino_id", nullable = false)
    private Etapa etapaDestino;

    @Column(nullable = false)
    private Integer diasNaEtapa;

    private LocalTime horarioMovimentacao;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Situacao situacao;

    private String descricao;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public String horarioMovimentacao() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return this.horarioMovimentacao.format(formatter);
    }

}

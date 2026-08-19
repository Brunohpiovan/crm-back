package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.ProcessoSituacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Processo judicial consultado/vinculado via Escavador (v2). numeroCnj é único por empresa e é a
 * chave usada pelo Prompt 2 para casar callbacks de monitoramento recebidos com este registro —
 * ver ProcessoRepository.findByNumeroCnjIgnoringTenant.
 */
@Getter
@Setter
@Entity
@Table(name = "processo")
@NoArgsConstructor
@AllArgsConstructor
public class Processo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "numero_cnj", nullable = false, length = 25)
    private String numeroCnj;

    @Column(name = "tribunal", length = 20)
    private String tribunal;

    @Column(name = "instancia", length = 60)
    private String instancia;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 20)
    private ProcessoSituacao situacao = ProcessoSituacao.DESCONHECIDO;

    @Column(name = "valor_causa")
    private BigDecimal valorCausa;

    @Column(name = "data_distribuicao")
    private LocalDate dataDistribuicao;

    @Column(name = "area", length = 100)
    private String area;

    @Column(name = "assunto", length = 255)
    private String assunto;

    @Column(name = "resumo_ia", columnDefinition = "TEXT")
    private String resumoIa;

    @Column(name = "resumo_ia_gerado_em")
    private LocalDateTime resumoIaGeradoEm;

    @Column(name = "ultima_consulta_em")
    private LocalDateTime ultimaConsultaEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}

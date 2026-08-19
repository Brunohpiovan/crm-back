package com.juridiqsystem.crm.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ledger interno do que cada empresa consumiu da conta única do juriq-crm na Escavador. É um
 * mecanismo de segurança/auditoria contra gasto descontrolado por bug — NÃO é a unidade
 * comercializada ao cliente (essa é a quantidade de processos monitorados simultaneamente, ver
 * Empresa.processosMonitoradosLimite) e não é exibido como tal na UI.
 *
 * <p>Sem @TenantId de propósito: é alimentado por @EventListener a partir de chamadas que também
 * ocorrem fora de uma requisição com tenant resolvido (scheduler, callback público), e a própria
 * linha já carrega o empresaId vindo do evento.</p>
 */
@Schema(description = "Lançamento no ledger interno de consumo da API da Escavador, por empresa.")
@Getter
@Setter
@Entity
@Table(name = "escavador_credito_lancamento")
@NoArgsConstructor
public class EscavadorCreditoLancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Null quando a chamada não pôde ser atribuída a nenhuma empresa (ex.: reconciliação disparada sem tenant resolvido) — o lançamento continua sendo registrado.")
    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "endpoint", nullable = false, length = 255)
    private String endpoint;

    @Column(name = "custo_centavos", nullable = false)
    private Integer custoCentavos;

    @Column(name = "sucesso", nullable = false)
    private Boolean sucesso;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public EscavadorCreditoLancamento(Long empresaId, String endpoint, Integer custoCentavos, boolean sucesso) {
        this.empresaId = empresaId;
        this.endpoint = endpoint;
        this.custoCentavos = custoCentavos == null ? 0 : custoCentavos;
        this.sucesso = sucesso;
        this.criadoEm = LocalDateTime.now();
    }
}

package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.ProcessoMonitoramento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessoMonitoramentoRepository extends JpaRepository<ProcessoMonitoramento, Long> {

    Optional<ProcessoMonitoramento> findByProcessoId(Long processoId);

    /** Base da checagem de cota do plano (processos monitorados simultaneamente). */
    long countByEmpresaIdAndAtivoTrue(Long empresaId);

    /**
     * Monitoramentos ligados cuja assinatura na Escavador nunca foi confirmada — a criação falhou
     * por erro transitório e precisa ser retentada pelo EscavadorMonitoramentoScheduler.
     *
     * <p>Nativa de propósito, mesmo motivo de
     * WhatsAppIntegrationRepository.findByPhoneNumberIdIgnoringTenant: roda no scheduler, sem
     * requisição HTTP e portanto sem tenant resolvido — uma query JPQL seria filtrada pelo
     * TenantIdentifierResolver e nunca encontraria nada. O tenant de cada linha é lido do próprio
     * empresa_id e reaplicado com TenantContext.runAs antes de qualquer escrita.</p>
     */
    @Query(value = """
            SELECT * FROM processo_monitoramento
            WHERE ativo = true AND escavador_monitoramento_id IS NULL
            ORDER BY ativado_em
            LIMIT :limite
            """, nativeQuery = true)
    List<ProcessoMonitoramento> findPendentesDeConfirmacaoIgnoringTenant(@Param("limite") int limite);
}

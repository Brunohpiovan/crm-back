package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.IntimacaoMonitoramento;
import com.juridiqsystem.crm.model.enums.Uf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntimacaoMonitoramentoRepository extends JpaRepository<IntimacaoMonitoramento, Long> {

    Optional<IntimacaoMonitoramento> findByOabNumeroAndOabUf(String oabNumero, Uf oabUf);

    List<IntimacaoMonitoramento> findAllByOrderByAtivadoEmDesc();

    /** Base da checagem de cota do plano (OABs monitoradas simultaneamente) — mesmo uso de ProcessoMonitoramentoRepository.countByEmpresaIdAndAtivoTrue. */
    long countByEmpresaIdAndAtivoTrue(Long empresaId);

    /**
     * Resolve o tenant de um callback de diário a partir do id da assinatura na Escavador — mesmo
     * papel de ProcessoMonitoramentoRepository.findPendentesDeConfirmacaoIgnoringTenant. Nativa de
     * propósito: o webhook roda sem tenant resolvido, então uma query JPQL não encontraria nada
     * (seria filtrada pelo TenantIdentifierResolver).
     */
    @Query(value = "SELECT * FROM intimacao_monitoramento WHERE escavador_monitoramento_id = :id", nativeQuery = true)
    Optional<IntimacaoMonitoramento> findByEscavadorMonitoramentoIdIgnoringTenant(@Param("id") String id);

    /**
     * OABs ligadas cuja assinatura na Escavador nunca foi confirmada — a criação falhou por erro
     * transitório e precisa ser retentada pelo IntimacaoMonitoramentoScheduler. Nativa pelo mesmo
     * motivo de findPendentesDeConfirmacaoIgnoringTenant em ProcessoMonitoramentoRepository: roda
     * no scheduler, fora de qualquer tenant resolvido.
     */
    @Query(value = """
            SELECT * FROM intimacao_monitoramento
            WHERE ativo = true AND escavador_monitoramento_id IS NULL
            ORDER BY ativado_em
            LIMIT :limite
            """, nativeQuery = true)
    List<IntimacaoMonitoramento> findPendentesDeConfirmacaoIgnoringTenant(@Param("limite") int limite);

    /**
     * OABs ativas e já confirmadas na Escavador — base da sincronização diária de aparições
     * (IntimacaoMonitoramentoScheduler.sincronizarTodasAtivas), rede de segurança para quando o
     * callback falhou silenciosamente para alguma delas. Nativa pelo mesmo motivo das outras
     * queries deste repositório usadas por scheduler: roda fora de qualquer tenant resolvido.
     */
    @Query(value = """
            SELECT * FROM intimacao_monitoramento
            WHERE ativo = true AND escavador_monitoramento_id IS NOT NULL
            ORDER BY ativado_em
            LIMIT :limite
            """, nativeQuery = true)
    List<IntimacaoMonitoramento> findAtivasConfirmadasIgnoringTenant(@Param("limite") int limite);
}

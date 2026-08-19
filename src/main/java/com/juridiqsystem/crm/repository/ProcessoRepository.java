package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Processo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessoRepository extends JpaRepository<Processo, Long> {

    Optional<Processo> findByPublicId(String publicId);

    Optional<Processo> findByEmpresaIdAndNumeroCnj(Long empresaId, String numeroCnj);

    /**
     * Base da tela /processos: só processos com pelo menos um vínculo em oportunidade_processo
     * (um Processo obtido só por "buscar-por-cnj", sem nunca ser vinculado, não deve aparecer na
     * lista geral). Busca livre por número CNJ ou nome do cliente da oportunidade vinculada.
     */
    @Query("""
    SELECT DISTINCT p FROM Processo p
    JOIN OportunidadeProcesso op ON op.processo = p
    LEFT JOIN op.oportunidade o
    LEFT JOIN o.cliente c
    WHERE (:search IS NULL OR :search = ''
        OR LOWER(p.numeroCnj) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :search, '%')))
    ORDER BY p.criadoEm DESC
    """)
    List<Processo> findVinculadosComFiltro(@Param("search") String search);

    /**
     * Nativa de propósito, mesmo motivo de WhatsAppIntegrationRepository.findByPhoneNumberIdIgnoringTenant:
     * chamada em contexto sem tenant conhecido (webhook/callback — ver Prompt 2). Uma query JPQL
     * normal seria filtrada pelo TenantIdentifierResolver (sentinela -1) e nunca encontraria nada.
     * numero_cnj é único por empresa, não globalmente — LIMIT 1 resolve a ambiguidade entre
     * empresas distintas monitorando o mesmo processo público, priorizando o registro mais antigo.
     */
    @Query(value = "SELECT * FROM processo WHERE numero_cnj = :numeroCnj ORDER BY id ASC LIMIT 1", nativeQuery = true)
    Optional<Processo> findByNumeroCnjIgnoringTenant(@Param("numeroCnj") String numeroCnj);
}

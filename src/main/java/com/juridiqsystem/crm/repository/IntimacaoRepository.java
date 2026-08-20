package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Intimacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IntimacaoRepository extends JpaRepository<Intimacao, Long> {

    boolean existsByChaveDedupe(String chaveDedupe);

    /**
     * Listagem paginada com filtros opcionais de lida/não-lida e advogado dono da OAB — join até
     * {@code intimacaoMonitoramento.usuarioAdvogado} sem duplicar o vínculo na tabela intimacao,
     * conforme pedido no prompt. Intimacao não tem @TenantId próprio (ver comentário na entidade),
     * mas o JOIN com intimacaoMonitoramento (que TEM @TenantId) já restringe o resultado ao tenant
     * atual automaticamente — o Hibernate aplica a restrição de tenant a qualquer query que toque
     * essa entidade, inclusive via join.
     */
    @Query(value = """
            SELECT i FROM Intimacao i JOIN FETCH i.intimacaoMonitoramento im
            WHERE (:lida IS NULL OR (:lida = TRUE AND i.lidaEm IS NOT NULL) OR (:lida = FALSE AND i.lidaEm IS NULL))
              AND (:usuarioAdvogadoId IS NULL OR im.usuarioAdvogado.id = :usuarioAdvogadoId)
            ORDER BY i.criadoEm DESC
            """,
            countQuery = """
            SELECT COUNT(i) FROM Intimacao i JOIN i.intimacaoMonitoramento im
            WHERE (:lida IS NULL OR (:lida = TRUE AND i.lidaEm IS NOT NULL) OR (:lida = FALSE AND i.lidaEm IS NULL))
              AND (:usuarioAdvogadoId IS NULL OR im.usuarioAdvogado.id = :usuarioAdvogadoId)
            """)
    Page<Intimacao> buscar(@Param("lida") Boolean lida, @Param("usuarioAdvogadoId") Long usuarioAdvogadoId, Pageable pageable);

    /**
     * Usada por marcarComoLida: Intimacao não tem @TenantId próprio, então um findById() puro não
     * seria filtrado por tenant. O JOIN com intimacaoMonitoramento restaura essa garantia (mesmo
     * racional de buscar() acima).
     */
    @Query("SELECT i FROM Intimacao i JOIN FETCH i.intimacaoMonitoramento im WHERE i.id = :id")
    Optional<Intimacao> findByIdComMonitoramento(@Param("id") Long id);
}

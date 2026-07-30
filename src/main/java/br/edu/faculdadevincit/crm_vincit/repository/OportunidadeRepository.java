package br.edu.faculdadevincit.crm_vincit.repository;
import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OportunidadeRepository extends JpaRepository<Oportunidade, Long> {

    @Query("""
    SELECT o FROM Oportunidade o
    LEFT JOIN FETCH o.etapa
    LEFT JOIN FETCH o.criador
    LEFT JOIN FETCH o.cliente
    """)
    Page<Oportunidade> findAllWithDetails(Pageable pageable);

    @Query("""
    SELECT DISTINCT o FROM Oportunidade o
    LEFT JOIN FETCH o.etapa
    LEFT JOIN FETCH o.criador
    LEFT JOIN FETCH o.cliente
    LEFT JOIN FETCH o.tags
    WHERE o.id = :id
    """)
    Optional<Oportunidade> findByIdWithDetails(@Param("id") Long id);

    @Query("""
    SELECT DISTINCT o FROM Oportunidade o
    LEFT JOIN FETCH o.etapa
    LEFT JOIN FETCH o.criador
    LEFT JOIN FETCH o.cliente
    LEFT JOIN FETCH o.tags
    WHERE o.cliente.id = :clienteId AND o.criador IS NULL
    """)
    Optional<Oportunidade> findByClienteIdAndCriadorIsNull(@Param("clienteId") Long clienteId);

    @Query("""
    SELECT DISTINCT o FROM Oportunidade o
    LEFT JOIN FETCH o.etapa
    LEFT JOIN FETCH o.criador
    LEFT JOIN FETCH o.cliente
    LEFT JOIN FETCH o.tags
    WHERE o.etapa.id = :etapaId
    ORDER BY o.indice ASC
    """)
    List<Oportunidade> findCardsByEtapaId(@Param("etapaId") Long etapaId);

    @Query("""
    SELECT DISTINCT o FROM Oportunidade o
    LEFT JOIN FETCH o.etapa
    LEFT JOIN FETCH o.criador
    LEFT JOIN FETCH o.cliente
    LEFT JOIN FETCH o.tags
    WHERE o.etapa.id IN :etapaIds
    ORDER BY o.etapa.id ASC, o.indice ASC
    """)
    List<Oportunidade> findCardsByEtapaIdIn(@Param("etapaIds") List<Long> etapaIds);

    @Query("""
    SELECT DISTINCT o FROM Oportunidade o
    LEFT JOIN FETCH o.etapa
    LEFT JOIN FETCH o.criador
    LEFT JOIN FETCH o.cliente
    LEFT JOIN FETCH o.tags
    WHERE o.etapa.id IN :etapaIds
      AND o.situacao IN :situacoes
    ORDER BY o.etapa.id ASC, o.indice ASC
    """)
    List<Oportunidade> findCardsByEtapaIdInAndSituacaoIn(
            @Param("etapaIds") List<Long> etapaIds,
            @Param("situacoes") List<SituacaoOportunidade> situacoes);

    @Query("""
    SELECT DISTINCT o FROM Oportunidade o
    LEFT JOIN FETCH o.etapa
    LEFT JOIN FETCH o.criador
    LEFT JOIN FETCH o.cliente
    LEFT JOIN FETCH o.tags
    WHERE o.etapa.id IN :etapaIds
      AND o.situacao IN :situacoes
      AND EXISTS (SELECT 1 FROM o.tags t WHERE t.id IN :tagIds)
    ORDER BY o.etapa.id ASC, o.indice ASC
    """)
    List<Oportunidade> findCardsByEtapaIdInAndSituacaoInAndTagIdsIn(
            @Param("etapaIds") List<Long> etapaIds,
            @Param("situacoes") List<SituacaoOportunidade> situacoes,
            @Param("tagIds") List<Long> tagIds);

    @Query("""
    SELECT o FROM Oportunidade o
    WHERE o.etapa.id = :etapaOrigemId
      AND o.dataEntradaEtapa <= :dataLimite
""")
    List<Oportunidade> findElegiveisParaMovimentacao(
            @Param("etapaOrigemId") Long etapaOrigemId,
            @Param("dataLimite") LocalDateTime dataLimite
    );

    long countByEtapa(Etapa etapa);

}

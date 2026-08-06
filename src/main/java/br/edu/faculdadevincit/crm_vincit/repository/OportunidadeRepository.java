package br.edu.faculdadevincit.crm_vincit.repository;
import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardFiltroRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardFunilEtapaResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardOrigemResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardRankingOportunidadeRow;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    SELECT DISTINCT o FROM Oportunidade o
    LEFT JOIN FETCH o.etapa
    LEFT JOIN FETCH o.criador
    LEFT JOIN FETCH o.cliente
    LEFT JOIN FETCH o.tags
    WHERE o.etapa.id = :etapaOrigemId
      AND o.dataEntradaEtapa <= :dataLimite
""")
    List<Oportunidade> findElegiveisParaMovimentacao(
            @Param("etapaOrigemId") Long etapaOrigemId,
            @Param("dataLimite") LocalDateTime dataLimite
    );

    long countByEtapa(Etapa etapa);

    @Query("SELECT o.url_anexo FROM Oportunidade o WHERE o.id = :id")
    Optional<String> findUrlAnexoById(@Param("id") Long id);

    // --- Agregações para o Dashboard (ver DashboardService) ---

    /**
     * Filtros comuns às agregações de Oportunidade: período (data_criacao), pipeline
     * permitido/selecionado, responsável, origem e tags. situacao é passada à parte pois cada
     * chamador usa um valor diferente (ABERTO/GANHO/PERDIDO).
     */
    @Query("""
    SELECT COALESCE(SUM(o.valor), 0) FROM Oportunidade o
    WHERE o.situacao = :situacao
      AND o.data_criacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND o.etapa.funil.id IN :#{#filtro.funilIdsPermitidos}
      AND (:#{#filtro.pipelineId} IS NULL OR o.etapa.funil.id = :#{#filtro.pipelineId})
      AND (:#{#filtro.userIds.size()} = 0 OR o.criador.id IN :#{#filtro.userIds})
      AND (:#{#filtro.origin.size()} = 0 OR o.origem IN :#{#filtro.origin})
      AND (:#{#filtro.tagIds.size()} = 0 OR EXISTS (SELECT 1 FROM o.tags t WHERE t.id IN :#{#filtro.tagIds}))
    """)
    BigDecimal sumValorPorSituacao(@Param("filtro") DashboardFiltroRequest filtro, @Param("situacao") SituacaoOportunidade situacao);

    @Query("""
    SELECT COUNT(o) FROM Oportunidade o
    WHERE o.situacao = :situacao
      AND o.data_criacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND o.etapa.funil.id IN :#{#filtro.funilIdsPermitidos}
      AND (:#{#filtro.pipelineId} IS NULL OR o.etapa.funil.id = :#{#filtro.pipelineId})
      AND (:#{#filtro.userIds.size()} = 0 OR o.criador.id IN :#{#filtro.userIds})
      AND (:#{#filtro.origin.size()} = 0 OR o.origem IN :#{#filtro.origin})
      AND (:#{#filtro.tagIds.size()} = 0 OR EXISTS (SELECT 1 FROM o.tags t WHERE t.id IN :#{#filtro.tagIds}))
    """)
    long countPorSituacao(@Param("filtro") DashboardFiltroRequest filtro, @Param("situacao") SituacaoOportunidade situacao);

    /**
     * LEFT JOIN com as condições no ON (não no WHERE) para que etapas sem nenhuma oportunidade
     * no período ainda apareçam no funil, com quantidade/valor zerados — "retornar todas as
     * etapas" é requisito explícito. ordem/percentual vêm com placeholder (0) e são recalculados
     * em DashboardService, que conhece o conjunto completo de etapas retornadas.
     */
    @Query("""
    SELECT new br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardFunilEtapaResponse(
        e.id, e.nome, 0, COUNT(o), COALESCE(SUM(o.valor), 0), 0.0)
    FROM Etapa e
    LEFT JOIN e.oportunidades o
        ON o.situacao <> br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade.LIXEIRA
        AND o.data_criacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
        AND (:#{#filtro.status.size()} = 0 OR o.situacao IN :#{#filtro.status})
        AND (:#{#filtro.userIds.size()} = 0 OR o.criador.id IN :#{#filtro.userIds})
        AND (:#{#filtro.origin.size()} = 0 OR o.origem IN :#{#filtro.origin})
        AND (:#{#filtro.tagIds.size()} = 0 OR EXISTS (SELECT 1 FROM o.tags t WHERE t.id IN :#{#filtro.tagIds}))
    WHERE e.funil.id IN :#{#filtro.funilIdsPermitidos}
      AND (:#{#filtro.pipelineId} IS NULL OR e.funil.id = :#{#filtro.pipelineId})
    GROUP BY e.id, e.nome, e.funil.id
    ORDER BY e.funil.id ASC, e.id ASC
    """)
    List<DashboardFunilEtapaResponse> funilPorEtapa(@Param("filtro") DashboardFiltroRequest filtro);

    @Query("""
    SELECT new br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardOrigemResponse(o.origem, COUNT(o), 0.0)
    FROM Oportunidade o
    WHERE o.situacao <> br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade.LIXEIRA
      AND o.data_criacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND o.etapa.funil.id IN :#{#filtro.funilIdsPermitidos}
      AND (:#{#filtro.status.size()} = 0 OR o.situacao IN :#{#filtro.status})
      AND (:#{#filtro.pipelineId} IS NULL OR o.etapa.funil.id = :#{#filtro.pipelineId})
      AND (:#{#filtro.userIds.size()} = 0 OR o.criador.id IN :#{#filtro.userIds})
      AND (:#{#filtro.tagIds.size()} = 0 OR EXISTS (SELECT 1 FROM o.tags t WHERE t.id IN :#{#filtro.tagIds}))
    GROUP BY o.origem
    ORDER BY COUNT(o) DESC
    """)
    List<DashboardOrigemResponse> leadsPorOrigem(@Param("filtro") DashboardFiltroRequest filtro);

    @Query("""
    SELECT new br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardRankingOportunidadeRow(
        u.id, u.nome,
        COUNT(CASE WHEN o.situacao = br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade.GANHO THEN o.id END),
        COUNT(CASE WHEN o.situacao = br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade.PERDIDO THEN o.id END),
        COALESCE(SUM(CASE WHEN o.situacao = br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade.GANHO THEN o.valor END), 0))
    FROM Oportunidade o JOIN o.criador u
    WHERE o.situacao IN (br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade.GANHO, br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade.PERDIDO)
      AND o.data_criacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND o.etapa.funil.id IN :#{#filtro.funilIdsPermitidos}
      AND (:#{#filtro.pipelineId} IS NULL OR o.etapa.funil.id = :#{#filtro.pipelineId})
      AND (:#{#filtro.userIds.size()} = 0 OR u.id IN :#{#filtro.userIds})
      AND (:#{#filtro.origin.size()} = 0 OR o.origem IN :#{#filtro.origin})
      AND (:#{#filtro.tagIds.size()} = 0 OR EXISTS (SELECT 1 FROM o.tags t WHERE t.id IN :#{#filtro.tagIds}))
    GROUP BY u.id, u.nome
    """)
    List<DashboardRankingOportunidadeRow> rankingOportunidadesPorUsuario(@Param("filtro") DashboardFiltroRequest filtro);

    @Query("""
    SELECT COUNT(o) FROM Oportunidade o
    WHERE o.etapa.id IN :etapaIds
      AND o.situacao <> br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade.LIXEIRA
    """)
    long countPorEtapaIdIn(@Param("etapaIds") List<Long> etapaIds);

    /**
     * Nativa, mesmo motivo/padrão das queries diárias de ProtocoloRepository (GROUP BY DATE(...),
     * ver DASHBOARD.md §5): JPQL não trunca datetime->date de forma portável e segura para
     * projeção em record. Filtros espelham sumValorPorSituacao (pipeline/responsável/origem/tag),
     * com o mesmo padrão flag+lista-dummy para filtros de coleção opcionais (IN () é erro de
     * sintaxe em SQL nativo no MySQL quando a coleção está vazia). Usado para o mini-gráfico do
     * card "Valor em negociação" no dashboard — é um proxy de atividade (valor criado por dia),
     * não uma reconstrução histórica do saldo total em aberto (o schema não guarda isso).
     */
    @Query(value = """
    SELECT DATE(o.data_criacao) AS dia, COALESCE(SUM(o.valor), 0) AS valor
    FROM oportunidade o
    JOIN etapa e ON e.id = o.card_id
    WHERE o.situacao = 'ABERTO'
      AND o.data_criacao BETWEEN :inicio AND :fim
      AND e.funil_id IN (:funilIds)
      AND (:pipelineId IS NULL OR e.funil_id = :pipelineId)
      AND (:semRestricaoUsuario = TRUE OR o.criador_id IN (:usuarioIds))
      AND (:semRestricaoOrigem = TRUE OR o.origem IN (:origens))
      AND (:semRestricaoTag = TRUE OR EXISTS (
            SELECT 1 FROM oportunidade_tag ot WHERE ot.oportunidade_id = o.id AND ot.tag_id IN (:tagIds)
          ))
    GROUP BY DATE(o.data_criacao)
    """, nativeQuery = true)
    List<DiaValorProjection> sumValorAbertoPorDia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
                                                    @Param("funilIds") List<Long> funilIds, @Param("pipelineId") Long pipelineId,
                                                    @Param("semRestricaoUsuario") boolean semRestricaoUsuario,
                                                    @Param("usuarioIds") List<Long> usuarioIds,
                                                    @Param("semRestricaoOrigem") boolean semRestricaoOrigem,
                                                    @Param("origens") List<String> origens,
                                                    @Param("semRestricaoTag") boolean semRestricaoTag,
                                                    @Param("tagIds") List<Long> tagIds);

    interface DiaValorProjection {
        LocalDate getDia();
        BigDecimal getValor();
    }

}

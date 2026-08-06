package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.CadenciaFunil;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardFiltroRequest;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CadenciaFunilRepository extends JpaRepository<CadenciaFunil, Long> {

    @EntityGraph(attributePaths = {"funilOrigem", "etapaOrigem", "funilDestino", "etapaDestino"})
    Optional<CadenciaFunil> findByPublicId(String publicId);

    /**
     * Usada pelo scheduler: traz etapaOrigem/etapaDestino já carregadas porque o processamento de
     * cada cadência roda fora de uma transação única (cada movimentação de oportunidade tem sua
     * própria transação, para isolar falha por item), então os relacionamentos LAZY não podem mais
     * ser acessados depois desta consulta retornar.
     */
    @Query("""
    SELECT c FROM CadenciaFunil c
    JOIN FETCH c.funilOrigem
    JOIN FETCH c.etapaOrigem
    JOIN FETCH c.funilDestino
    JOIN FETCH c.etapaDestino
    WHERE c.situacao = :situacao
    """)
    List<CadenciaFunil> findAllBySituacaoWithDetails(@Param("situacao") Situacao situacao);

    @Override
    @EntityGraph(attributePaths = {"funilOrigem", "etapaOrigem", "funilDestino", "etapaDestino"})
    Optional<CadenciaFunil> findById(Long id);

    @Query("""
    SELECT c FROM CadenciaFunil c
    JOIN FETCH c.funilOrigem
    JOIN FETCH c.etapaOrigem
    JOIN FETCH c.funilDestino
    JOIN FETCH c.etapaDestino
    """)
    List<CadenciaFunil> findAllWithDetails();

    // --- Agregações para o Dashboard (ver DashboardService) ---

    @Query("""
    SELECT COUNT(c) FROM CadenciaFunil c
    WHERE c.situacao = :situacao
      AND (c.funilOrigem.id IN :#{#filtro.funilIdsPermitidos} OR c.funilDestino.id IN :#{#filtro.funilIdsPermitidos})
      AND (:#{#filtro.pipelineId} IS NULL OR c.funilOrigem.id = :#{#filtro.pipelineId} OR c.funilDestino.id = :#{#filtro.pipelineId})
    """)
    long countBySituacao(@Param("filtro") DashboardFiltroRequest filtro, @Param("situacao") Situacao situacao);

    /**
     * Cadências ATIVAS visíveis para o usuário (escopo de pipeline), com etapaOrigem/etapaDestino
     * já carregadas — usadas por DashboardService para calcular oportunidadesEmExecucao,
     * execucoesHoje (aproximação) e proximaExecucao a partir de horarioMovimentacao.
     */
    @Query("""
    SELECT c FROM CadenciaFunil c
    JOIN FETCH c.etapaOrigem
    JOIN FETCH c.etapaDestino
    WHERE c.situacao = br.edu.faculdadevincit.crm_vincit.model.enums.Situacao.ATIVA
      AND (c.funilOrigem.id IN :#{#filtro.funilIdsPermitidos} OR c.funilDestino.id IN :#{#filtro.funilIdsPermitidos})
      AND (:#{#filtro.pipelineId} IS NULL OR c.funilOrigem.id = :#{#filtro.pipelineId} OR c.funilDestino.id = :#{#filtro.pipelineId})
    """)
    List<CadenciaFunil> findAtivasVisiveis(@Param("filtro") DashboardFiltroRequest filtro);

}

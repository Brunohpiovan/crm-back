package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.OportunidadeProcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OportunidadeProcessoRepository extends JpaRepository<OportunidadeProcesso, Long> {

    @Query("""
    SELECT op FROM OportunidadeProcesso op
    JOIN FETCH op.processo
    JOIN FETCH op.vinculadoPor
    WHERE op.oportunidade.id = :oportunidadeId
    ORDER BY op.vinculadoEm DESC
    """)
    List<OportunidadeProcesso> findByOportunidadeId(@Param("oportunidadeId") Long oportunidadeId);

    Optional<OportunidadeProcesso> findByOportunidadeIdAndProcessoId(Long oportunidadeId, Long processoId);

    Optional<OportunidadeProcesso> findByOportunidadeIdAndProcessoPublicId(Long oportunidadeId, String processoPublicId);

    long countByOportunidadeId(Long oportunidadeId);

    /**
     * Contagem por oportunidade, para exibir "processos vinculados" nos cards do Kanban sem N+1 nem
     * JOIN FETCH direto em Oportunidade (que causaria produto cartesiano combinado com o fetch de
     * tags já existente na query de cards). Object[] é {oportunidadeId, total} por linha.
     */
    @Query("SELECT op.oportunidade.id, COUNT(op) FROM OportunidadeProcesso op WHERE op.oportunidade.id IN :oportunidadeIds GROUP BY op.oportunidade.id")
    List<Object[]> countByOportunidadeIdIn(@Param("oportunidadeIds") List<Long> oportunidadeIds);
}

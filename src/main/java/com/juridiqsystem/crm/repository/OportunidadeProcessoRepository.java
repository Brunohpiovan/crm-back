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
}

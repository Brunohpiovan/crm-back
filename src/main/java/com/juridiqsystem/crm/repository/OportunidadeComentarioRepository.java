package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.OportunidadeComentario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OportunidadeComentarioRepository extends JpaRepository<OportunidadeComentario, Long> {

    /**
     * JOIN FETCH no autor: sem isso o Hibernate devolve um proxy de Usuario que só é resolvido
     * fora da sessão (na hora de montar o DTO em OportunidadeComentarioService), estourando
     * LazyInitializationException — mesmo padrão de OportunidadeRepository.findAllWithDetails.
     */
    @Query("""
    SELECT c FROM OportunidadeComentario c
    LEFT JOIN FETCH c.autor
    WHERE c.oportunidadeId = :oportunidadeId
    ORDER BY c.criadoEm DESC
    """)
    Page<OportunidadeComentario> findByOportunidadeIdOrderByCriadoEmDesc(@Param("oportunidadeId") Long oportunidadeId, Pageable pageable);

    @Query("""
    SELECT c FROM OportunidadeComentario c
    LEFT JOIN FETCH c.autor
    WHERE c.publicId = :publicId
    """)
    Optional<OportunidadeComentario> findByPublicId(@Param("publicId") String publicId);
}

package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByCodigo(String codigo);

    Optional<Empresa> findByPublicId(String publicId);

    @Query("""
    SELECT e FROM Empresa e
    WHERE e.interna = false
    AND (:search IS NULL OR LOWER(e.nome) LIKE :search OR LOWER(e.codigo) LIKE :search)
    """)
    Page<Empresa> findByInternaFalseAndSearch(@Param("search") String search, Pageable pageable);
}

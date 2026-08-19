package com.juridiqsystem.crm.repository;
import com.juridiqsystem.crm.model.Funil;
import com.juridiqsystem.crm.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FunilRepository extends JpaRepository<Funil, Long> {
    Optional<Funil> findByPublicId(String publicId);

    List<Funil> findByFuncionariosContains(Usuario usuario);

    @Query("""
    SELECT DISTINCT f FROM Funil f
    LEFT JOIN FETCH f.etapas e
    WHERE f.id = :id
    """)
    Optional<Funil> findByIdWithEtapas(@Param("id") Long id);

    @Query("""
    SELECT DISTINCT f FROM Funil f
    LEFT JOIN FETCH f.etapas e
    WHERE f.publicId = :publicId
    """)
    Optional<Funil> findByPublicIdWithEtapas(@Param("publicId") String publicId);

}

package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Acesso;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcessoRepository extends JpaRepository<Acesso, Long> {

    Optional<Acesso> findByPublicId(String publicId);

    @EntityGraph(attributePaths = {"usuario"})
    List<Acesso> findByUsuarioId(Long usuarioId);

    @Override
    @EntityGraph(attributePaths = {"usuario"})
    List<Acesso> findAll();

}

package com.juridiqsystem.crm.repository;
import com.juridiqsystem.crm.model.Etapa;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EtapaRepository extends JpaRepository<Etapa, Long> {
    @EntityGraph(attributePaths = {"funil"})
    Optional<Etapa> findByPublicId(String publicId);

    List<Etapa> findByFunilIdOrderByPosicaoAscIdAsc(Long funilId);

    boolean existsByNomeAndFunilId(String nome, Long funilId);

    @Override
    @EntityGraph(attributePaths = {"funil"})
    List<Etapa> findAll();

    @Override
    @EntityGraph(attributePaths = {"funil"})
    Optional<Etapa> findById(Long id);
}

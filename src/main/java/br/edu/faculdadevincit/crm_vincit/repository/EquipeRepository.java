package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Equipe;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, Long> {

    @EntityGraph(attributePaths = {"membros"})
    Optional<Equipe> findByPublicId(String publicId);

    @Query("SELECT m.id FROM Equipe e JOIN e.membros m WHERE e.id = :equipeId")
    List<Long> findMembroIdsById(@Param("equipeId") Long equipeId);

    boolean existsByIdAndMembrosContains(Long id, Usuario usuario);

    @Override
    @EntityGraph(attributePaths = {"membros"})
    List<Equipe> findAll();
}

package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.CadenciaFunil;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CadenciaFunilRepository extends JpaRepository<CadenciaFunil, Long> {

    List<CadenciaFunil> findAllBySituacao(Situacao situacao);

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

}

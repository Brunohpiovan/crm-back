package br.edu.faculdadevincit.crm_vincit.repository;
import br.edu.faculdadevincit.crm_vincit.model.Funil;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FunilRepository extends JpaRepository<Funil, Long> {
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
    LEFT JOIN FETCH e.oportunidades o
    WHERE f.id = :id AND (o.situacao IN :situacoes OR o IS NULL)
    """)
    Optional<Funil> findByIdWithFilteredOportunidades(@Param("id") Long id,
                                                      @Param("situacoes") List<SituacaoOportunidade> situacoes);

}

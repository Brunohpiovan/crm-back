package br.edu.faculdadevincit.crm_vincit.repository;
import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import br.edu.faculdadevincit.crm_vincit.model.Oportunidade;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OportunidadeRepository extends JpaRepository<Oportunidade, Long> {
    List<Oportunidade> findByEtapaId(Long cardId);

    List<Oportunidade> findByClienteId(Long clienteId);

    Optional<Oportunidade> findByClienteIdAndCriadorIsNull(Long clienteId);

    @Query("""
    SELECT o FROM Oportunidade o
    WHERE o.etapa.id = :etapaOrigemId
      AND o.dataEntradaEtapa <= :dataLimite
""")
    List<Oportunidade> findElegiveisParaMovimentacao(
            @Param("etapaOrigemId") Long etapaOrigemId,
            @Param("dataLimite") LocalDateTime dataLimite
    );

    long countByEtapa(Etapa etapa);

    @Query("SELECT DISTINCT o FROM Oportunidade o " +
            "JOIN o.tags t " +
            "WHERE o.etapa.id = :etapaId " +
            "AND o.situacao IN :situacoes " +
            "AND t.id IN :tagIds")
    List<Oportunidade> findByEtapaIdAndSituacaoInAndTagIdsIn(
            @Param("etapaId") Long etapaId,
            @Param("situacoes") List<SituacaoOportunidade> situacoes,
            @Param("tagIds") List<Long> tagIds);


    List<Oportunidade> findByEtapaIdAndSituacaoIn(Long etapaId, List<SituacaoOportunidade> situacoes);


}

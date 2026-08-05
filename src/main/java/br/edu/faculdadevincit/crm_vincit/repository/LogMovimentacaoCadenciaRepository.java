package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.LogMovimentacaoCadencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogMovimentacaoCadenciaRepository extends JpaRepository<LogMovimentacaoCadencia, Long> {

    long countByExecutadoEmBetweenAndEtapaDestinoIdIn(LocalDateTime inicio, LocalDateTime fim, List<Long> etapaDestinoIds);
}

package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.ProcessoMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProcessoMovimentacaoRepository extends JpaRepository<ProcessoMovimentacao, Long> {

    List<ProcessoMovimentacao> findByProcessoIdOrderByDataMovimentacaoDesc(Long processoId);

    boolean existsByProcessoIdAndDataMovimentacaoAndHashConteudo(Long processoId, LocalDateTime dataMovimentacao, String hashConteudo);
}

package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.OportunidadeHistorico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OportunidadeHistoricoRepository extends JpaRepository<OportunidadeHistorico, Long> {
    Page<OportunidadeHistorico> findByOportunidadeIdOrderByCriadoEmDesc(Long oportunidadeId, Pageable pageable);
}

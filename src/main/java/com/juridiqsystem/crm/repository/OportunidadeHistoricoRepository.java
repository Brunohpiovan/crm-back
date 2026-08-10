package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.OportunidadeHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OportunidadeHistoricoRepository extends JpaRepository<OportunidadeHistorico, Long> {
    List<OportunidadeHistorico> findByOportunidadeIdOrderByCriadoEmDesc(Long oportunidadeId);
}

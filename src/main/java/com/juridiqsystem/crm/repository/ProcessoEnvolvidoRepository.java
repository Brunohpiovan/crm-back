package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.ProcessoEnvolvido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessoEnvolvidoRepository extends JpaRepository<ProcessoEnvolvido, Long> {

    List<ProcessoEnvolvido> findByProcessoIdOrderById(Long processoId);

    @Modifying
    @Query("DELETE FROM ProcessoEnvolvido e WHERE e.processo.id = :processoId")
    void deleteAllByProcessoId(@Param("processoId") Long processoId);
}

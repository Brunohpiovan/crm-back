package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.ProcessoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessoDocumentoRepository extends JpaRepository<ProcessoDocumento, Long> {

    List<ProcessoDocumento> findByProcessoIdOrderByDataDocumentoDesc(Long processoId);

    Optional<ProcessoDocumento> findByIdAndProcessoId(Long id, Long processoId);

    boolean existsByProcessoIdAndEscavadorDocumentoId(Long processoId, String escavadorDocumentoId);
}

package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.EscavadorCallbackEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EscavadorCallbackEventoRepository extends JpaRepository<EscavadorCallbackEvento, Long> {
}

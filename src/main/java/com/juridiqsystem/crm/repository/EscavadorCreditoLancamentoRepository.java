package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.EscavadorCreditoLancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EscavadorCreditoLancamentoRepository extends JpaRepository<EscavadorCreditoLancamento, Long> {
}

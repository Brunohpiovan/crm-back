package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByCodigo(String codigo);

    Page<Empresa> findByInternaFalse(Pageable pageable);
}

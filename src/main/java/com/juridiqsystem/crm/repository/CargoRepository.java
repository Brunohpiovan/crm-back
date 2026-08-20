package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Cargo é @TenantId: toda query já é filtrada automaticamente pela empresa da requisição (ver
 * TenantIdentifierResolver). O empresaId explícito nas assinaturas abaixo é redundante de
 * propósito — deixa a intenção visível na chamada e garante que um cargo de outra empresa nunca
 * seja resolvido nem por engano nem se o tenant algum dia deixar de estar populado.
 */
@Repository
public interface CargoRepository extends JpaRepository<Cargo, Long> {

    /**
     * LEFT JOIN FETCH nas permissões: sem isso, o @ElementCollection EAGER dispara uma query
     * extra por cargo (N+1) na listagem da tela de cargos.
     */
    @Query("SELECT DISTINCT c FROM Cargo c LEFT JOIN FETCH c.permissoes WHERE c.empresaId = :empresaId ORDER BY c.nome")
    List<Cargo> findByEmpresaId(@Param("empresaId") Long empresaId);

    Optional<Cargo> findByEmpresaIdAndPublicId(Long empresaId, String publicId);

    boolean existsByEmpresaIdAndNomeIgnoreCase(Long empresaId, String nome);

    boolean existsByEmpresaIdAndNomeIgnoreCaseAndIdNot(Long empresaId, String nome, Long id);

    Optional<Cargo> findByEmpresaIdAndAdministradorTrue(Long empresaId);
}

package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.CriadorDto;
import com.juridiqsystem.crm.model.dtos.EmpresaUsuarioCountProjection;
import com.juridiqsystem.crm.model.dtos.UsuarioAllContactsDTO;
import com.juridiqsystem.crm.model.dtos.UsuarioAllDTO;
import com.juridiqsystem.crm.model.dtos.UsuarioContatoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByPublicId(String publicId);

    List<Usuario> findAllByPublicIdIn(List<String> publicIds);

    Optional<UserDetails> findByLogin(String login);

    boolean existsByLoginIgnoreCase(String login);
    boolean existsByLoginIgnoreCaseAndIdNot(String login, Long id);
    boolean existsByCpf(String cpf);
    boolean existsByCpfAndIdNot(String cpf, Long id);

    /** Usado por CargoService.excluir: um cargo com usuários vinculados não pode ser removido. */
    boolean existsByCargoId(Long cargoId);

    /**
     * Nativa de propósito: login/cpf são únicos por Empresa agora, e esta é a query usada
     * ANTES de qualquer autenticação (AuthorizationService.loadUserByUsername) — o
     * TenantContext ainda não foi populado nesse ponto (é o próprio login que descobre qual é
     * a empresa, via codigoEmpresa), então uma query JPQL normal seria filtrada pelo
     * TenantIdentifierResolver e nunca encontraria ninguém (ver @TenantId em Usuario). Nativa
     * escapa desse filtro de propósito, não por descuido.
     */
    @Query(value = "SELECT u.* FROM usuario u JOIN empresa e ON e.id = u.empresa_id WHERE e.codigo = :codigoEmpresa AND u.login = :login", nativeQuery = true)
    Optional<Usuario> findByEmpresaCodigoAndLogin(@Param("codigoEmpresa") String codigoEmpresa, @Param("login") String login);

    /**
     * Nativa de propósito, mesmo motivo de findByEmpresaCodigoAndLogin: usada em
     * PasswordResetService.changePassord (POST /reset-password, permitAll), onde ainda não há
     * TenantContext populado — a prova de identidade nesse fluxo é o próprio token opaco
     * (PasswordResetToken), não uma sessão autenticada, então não dá pra confiar em nenhum
     * tenant já resolvido. findById normal (via @TenantId) simplesmente não encontraria
     * ninguém nesse ponto.
     */
    @Query(value = "SELECT * FROM usuario WHERE id = :id", nativeQuery = true)
    Optional<Usuario> findByIdIgnorandoTenant(@Param("id") Long id);

    /**
     * "Disponíveis para o funil" exclui quem já está nele e quem ocupa o cargo de administrador
     * da empresa (administrador enxerga todos os funis por definição, não precisa ser vinculado
     * a um). Antes o cargo a excluir vinha como parâmetro (UserRole.ADMINISTRADOR); agora a
     * condição é o flag do Cargo, já que o nome do cargo é livre por empresa.
     */
    @Query("""
    SELECT new com.juridiqsystem.crm.model.dtos.UsuarioContatoDto(u.publicId, u.nome, u.urlPicture)
    FROM Usuario u
    WHERE u.cargo.administrador = false
    AND u.id NOT IN (
        SELECT f.id FROM Funil fn JOIN fn.funcionarios f WHERE fn.id = :funilId
    )
    """)
    List<UsuarioContatoDto> findDisponiveisParaFunil(@Param("funilId") Long funilId);

    @Query("""
    SELECT new com.juridiqsystem.crm.model.dtos.UsuarioContatoDto(u.publicId, u.nome, u.urlPicture)
    FROM Usuario u
    WHERE u.id IN (
        SELECT f.id FROM Funil fn JOIN fn.funcionarios f WHERE fn.id = :funilId
    )
    """)
    List<UsuarioContatoDto> findFuncionariosDoFunil(@Param("funilId") Long funilId);

    @Query("""
    SELECT new com.juridiqsystem.crm.model.dtos.UsuarioAllDTO(u.publicId, u.nome, u.login, u.celular, u.cargo.publicId, u.cargo.nome, u.cargo.administrador, u.master, u.bloqueado, u.urlPicture, u.oabNumero, u.oabUf)
    FROM Usuario u
    WHERE (:search IS NULL OR LOWER(u.nome) LIKE :search OR LOWER(u.login) LIKE :search)
    """)
    Page<UsuarioAllDTO> findAllPaginado(@Param("search") String search, Pageable pageable);

    /**
     * Substitui findResumoByCargo(UserRole.ADMINISTRADOR): com cargo customizável, "administrador"
     * deixou de ser um nome de enum e passou a ser o flag do Cargo da empresa.
     */
    @Query("""
    SELECT new com.juridiqsystem.crm.model.dtos.UsuarioAllDTO(u.publicId, u.nome, u.login, u.celular, u.cargo.publicId, u.cargo.nome, u.cargo.administrador, u.master, u.bloqueado, u.urlPicture, u.oabNumero, u.oabUf)
    FROM Usuario u
    WHERE u.cargo.administrador = true
    """)
    List<UsuarioAllDTO> findResumoByCargoAdministradorTrue();

    @Query("""
    SELECT new com.juridiqsystem.crm.model.dtos.CriadorDto(u.publicId, u.nome, u.login, u.celular, u.urlPicture)
    FROM Usuario u
    """)
    List<CriadorDto> findAllCriadores();

    @Query("""
    SELECT DISTINCT new com.juridiqsystem.crm.model.dtos.UsuarioAllContactsDTO(u.publicId, u.nome, u.urlPicture)
    FROM ChatGrupo g
    JOIN g.usuarios u
    WHERE g.privado = true
    AND :userId IN (SELECT u2.id FROM g.usuarios u2)
    """)
    List<UsuarioAllContactsDTO> findAllContactsWithPrivateGroups(@Param("userId") Long userId);

    @Query("""
    SELECT new com.juridiqsystem.crm.model.dtos.UsuarioAllContactsDTO(u.publicId, u.nome, u.urlPicture)
    FROM Usuario u
    WHERE u.id != :userId
    AND u.id NOT IN (
        SELECT usu.id FROM ChatGrupo g
        JOIN g.usuarios usu
        WHERE g.privado = true
        AND :userId IN (SELECT u2.id FROM g.usuarios u2)
    )
    """)
    List<UsuarioAllContactsDTO> findUsuariosSemGrupoPrivadoComum(Long userId);

    @Query(value = "SELECT * FROM usuario WHERE bloqueado = false ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Usuario> findRandomUsuarioDisponivel();

    /**
     * Nativa de propósito: usada por EmpresaService.findAll (listagem /master/empresas) para
     * contar usuários ativos de várias empresas de uma vez. Como Usuario tem @TenantId, uma
     * query JPQL normal seria filtrada automaticamente pelo tenant da requisição atual (a
     * empresa interna do master) e nunca contaria as empresas-cliente. Só considera usuários não
     * bloqueados (bloqueado = true é inativação lógica, não deve entrar na contagem "atual").
     */
    @Query(value = """
    SELECT u.empresa_id AS empresaId,
           COUNT(*) AS totalUsuarios,
           SUM(CASE WHEN c.administrador = TRUE THEN 1 ELSE 0 END) AS totalAdmins
    FROM usuario u
    JOIN cargo c ON c.id = u.cargo_id
    WHERE u.empresa_id IN (:empresaIds) AND u.bloqueado = false
    GROUP BY u.empresa_id
    """, nativeQuery = true)
    List<EmpresaUsuarioCountProjection> countUsuariosPorEmpresa(@Param("empresaIds") List<Long> empresaIds);
}
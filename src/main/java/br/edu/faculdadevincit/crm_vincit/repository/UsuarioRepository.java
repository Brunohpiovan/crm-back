package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.CriadorDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioAllContactsDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioAllDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioContatoDto;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
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
    Optional<UserDetails> findByLogin(String login);

    boolean existsByLoginIgnoreCase(String login);
    boolean existsByLoginIgnoreCaseAndIdNot(String login, Long id);
    boolean existsByCpf(String cpf);
    boolean existsByCpfAndIdNot(String cpf, Long id);

    @Query("""
    SELECT new br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioContatoDto(u.id, u.nome, u.urlPicture)
    FROM Usuario u
    WHERE u.cargo <> :cargo
    AND u.id NOT IN (
        SELECT f.id FROM Funil fn JOIN fn.funcionarios f WHERE fn.id = :funilId
    )
    """)
    List<UsuarioContatoDto> findDisponiveisParaFunil(@Param("funilId") Long funilId, @Param("cargo") UserRole cargo);

    @Query("""
    SELECT new br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioAllDTO(u.id, u.nome, u.login, u.celular)
    FROM Usuario u
    WHERE u.bloqueado = false
    AND (:search IS NULL OR LOWER(u.nome) LIKE :search OR LOWER(u.login) LIKE :search)
    """)
    Page<UsuarioAllDTO> findAllAtivos(@Param("search") String search, Pageable pageable);

    @Query("""
    SELECT new br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioAllDTO(u.id, u.nome, u.login, u.celular)
    FROM Usuario u
    WHERE u.cargo = :cargo
    """)
    List<UsuarioAllDTO> findResumoByCargo(@Param("cargo") UserRole cargo);

    @Query("""
    SELECT new br.edu.faculdadevincit.crm_vincit.model.dtos.CriadorDto(u.id, u.nome, u.login, u.celular, u.urlPicture)
    FROM Usuario u
    """)
    List<CriadorDto> findAllCriadores();

    @Query("""
    SELECT DISTINCT new br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioAllContactsDTO(u.id, u.nome, u.urlPicture)
    FROM ChatGrupo g
    JOIN g.usuarios u
    WHERE g.privado = true
    AND :userId IN (SELECT u2.id FROM g.usuarios u2)
    """)
    List<UsuarioAllContactsDTO> findAllContactsWithPrivateGroups(@Param("userId") Long userId);

    @Query("""
    SELECT new br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioAllContactsDTO(u.id, u.nome, u.urlPicture)
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


}
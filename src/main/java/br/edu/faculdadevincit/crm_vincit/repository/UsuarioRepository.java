package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
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
    Optional<UserDetails> findByCpf(String cpf);
    List<Usuario> findByCargo(UserRole cargo);
    Optional<Usuario> findByCelular(String celular);

    @Query("SELECT u FROM Usuario u WHERE u.cargo <> :cargo")
    List<Usuario> findByNotCargo(@Param("cargo") UserRole cargo);

    @Query("SELECT DISTINCT u FROM ChatGrupo g " +
            "JOIN g.usuarios u " +
            "WHERE g.privado = true " +
            "AND :userId IN (SELECT u2.id FROM g.usuarios u2)")
    List<Usuario> findAllContactsWithPrivateGroups(@Param("userId") Long userId);

    @Query("""
    SELECT u FROM Usuario u
    WHERE u.id != :userId
    AND u.id NOT IN (
        SELECT usu.id FROM ChatGrupo g
        JOIN g.usuarios usu
        WHERE g.privado = true
        AND :userId IN (SELECT u2.id FROM g.usuarios u2)
    )
    """)
    List<Usuario> findUsuariosSemGrupoPrivadoComum(Long userId);

    @Query(value = "SELECT * FROM usuario WHERE bloqueado = false ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Usuario> findRandomUsuarioDisponivel();


}
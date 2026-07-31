package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.ChatGrupo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatGrupoRepository extends JpaRepository<ChatGrupo, Long> {

    @Override
    @EntityGraph(attributePaths = {"usuarios"})
    Optional<ChatGrupo> findById(Long id);

    @Query("""
        SELECT g
        FROM ChatGrupo g
        JOIN g.usuarios u1
        JOIN g.usuarios u2
        WHERE g.privado = true
          AND u1.id = :idUsuario1
          AND u2.id = :idUsuario2
          AND SIZE(g.usuarios) = 2
    """)
    Optional<ChatGrupo> findGrupoPrivadoByUsuarios(@Param("idUsuario1") Long idUsuario1,
                                                   @Param("idUsuario2") Long idUsuario2);

    @Query("""
    SELECT g
    FROM ChatGrupo g
    JOIN g.usuarios u
    WHERE g.privado = false
      AND u.id = :idUsuario
""")
    List<ChatGrupo> findGruposPublicosByUsuario(@Param("idUsuario") Long idUsuario);

}

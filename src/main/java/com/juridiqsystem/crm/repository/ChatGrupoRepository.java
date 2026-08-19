package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.ChatGrupo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatGrupoRepository extends JpaRepository<ChatGrupo, Long> {

    @EntityGraph(attributePaths = {"usuarios"})
    Optional<ChatGrupo> findByPublicId(String publicId);

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
    List<ChatGrupo> findGrupoPrivadoByUsuarios(@Param("idUsuario1") Long idUsuario1,
                                                @Param("idUsuario2") Long idUsuario2);

    @Query("""
    SELECT g
    FROM ChatGrupo g
    JOIN g.usuarios u
    WHERE g.privado = false
      AND u.id = :idUsuario
""")
    List<ChatGrupo> findGruposPublicosByUsuario(@Param("idUsuario") Long idUsuario);

    /**
     * Para o usuário idUsuario, o id do grupo privado (1-a-1) com cada um dos outrosPublicIds —
     * batched (uma query), usado para resolver a "última mensagem" na lista de contatos do chat
     * interno. Contatos sem grupo em comum (ex.: lista de "disponíveis") simplesmente não aparecem
     * no resultado.
     */
    @Query("""
    SELECT u2.publicId AS otherUserPublicId, g.id AS grupoId
    FROM ChatGrupo g
    JOIN g.usuarios u1
    JOIN g.usuarios u2
    WHERE g.privado = true
      AND u1.id = :idUsuario
      AND u2.publicId IN :outrosPublicIds
      AND u2.id <> :idUsuario
      AND SIZE(g.usuarios) = 2
    """)
    List<GrupoPrivadoProjection> findGruposPrivadosPorUsuarioEOutros(@Param("idUsuario") Long idUsuario,
                                                                       @Param("outrosPublicIds") List<String> outrosPublicIds);

    interface GrupoPrivadoProjection {
        String getOtherUserPublicId();
        Long getGrupoId();
    }

}

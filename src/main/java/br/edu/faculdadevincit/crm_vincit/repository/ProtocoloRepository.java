package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardFiltroRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardRankingProtocoloRow;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProtocoloRepository extends JpaRepository<Protocolo, Long> {

    @Override
    @EntityGraph(attributePaths = {"admin", "participante", "adminAnterior"})
    Optional<Protocolo> findById(Long id);

    @Query("SELECT p FROM Protocolo p JOIN FETCH p.admin JOIN FETCH p.participante LEFT JOIN FETCH p.adminAnterior WHERE ((p.admin.celular = :celular1 AND p.participante.celular = :celular2) OR (p.admin.celular = :celular2 AND p.participante.celular = :celular1)) AND p.status = :status")
    Optional<Protocolo> findByAdminCelularAndParticipanteCelularAndStatus(@Param("celular1") String celular1, @Param("celular2") String celular2, @Param("status") StatusProtocolo status);

    @Query("SELECT p FROM Protocolo p JOIN FETCH p.admin JOIN FETCH p.participante LEFT JOIN FETCH p.adminAnterior WHERE (p.admin.login = :login OR p.participante.login = :login)")
    List<Protocolo> findByAdminLoginOrParticipanteLogin(@Param("login") String login);

    @Query("""
    SELECT p.participante.id FROM Protocolo p
    WHERE p.status = br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo.ABERTO
      AND p.admin.id = :adminId
    """)
    List<Long> findParticipanteIdsComProtocoloAbertoPorAdmin(@Param("adminId") Long adminId);

    @Query(value = """
    SELECT p FROM Protocolo p JOIN FETCH p.admin JOIN FETCH p.participante LEFT JOIN FETCH p.adminAnterior
    WHERE (p.admin.login = :login OR p.participante.login = :login)
      AND (:search IS NULL
           OR CAST(p.id AS string) LIKE :search
           OR LOWER(p.admin.nome) LIKE :search
           OR LOWER(p.participante.nome) LIKE :search)
    """,
    countQuery = """
    SELECT COUNT(p) FROM Protocolo p
    WHERE (p.admin.login = :login OR p.participante.login = :login)
      AND (:search IS NULL
           OR CAST(p.id AS string) LIKE :search
           OR LOWER(p.admin.nome) LIKE :search
           OR LOWER(p.participante.nome) LIKE :search)
    """)
    Page<Protocolo> findByAdminLoginOrParticipanteLoginPaginado(@Param("login") String login, @Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Protocolo p JOIN FETCH p.admin JOIN FETCH p.participante LEFT JOIN FETCH p.adminAnterior WHERE (p.participante.celular = :celular) AND p.status = :status")
    Optional<Protocolo> findByCelularAndStatus(@Param("celular") String celular, @Param("status") StatusProtocolo status);

    @Query("SELECT p FROM Protocolo p JOIN FETCH p.admin JOIN FETCH p.participante LEFT JOIN FETCH p.adminAnterior WHERE p.participante.id = :usuarioId AND p.status = :status")
    Optional<Protocolo> findByParticipanteIdAndStatusAberto(@Param("usuarioId") Long usuarioId, @Param("status") StatusProtocolo status);


    @Query("""
    SELECT COUNT(p) FROM Protocolo p
    WHERE p.status = br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo.ABERTO
      AND p.dataCriacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND (:#{#filtro.userIds.size()} = 0 OR p.admin.id IN :#{#filtro.userIds})
    """)
    long countAbertos(@Param("filtro") DashboardFiltroRequest filtro);

    @Query("""
    SELECT COUNT(p) FROM Protocolo p
    WHERE p.status = br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo.ABERTO
      AND p.dataCriacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND p.dataCriacao <= :limiteRisco
      AND (:#{#filtro.userIds.size()} = 0 OR p.admin.id IN :#{#filtro.userIds})
    """)
    long countEmRisco(@Param("filtro") DashboardFiltroRequest filtro, @Param("limiteRisco") LocalDateTime limiteRisco);

    @Query("""
    SELECT AVG(CAST(FUNCTION('TIMESTAMPDIFF', MINUTE, p.dataCriacao, p.dataEncerramento) AS double)) FROM Protocolo p
    WHERE p.status = br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo.FECHADO
      AND p.dataCriacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND (:#{#filtro.userIds.size()} = 0 OR p.admin.id IN :#{#filtro.userIds})
    """)
    Double avgTempoAtendimentoMinutos(@Param("filtro") DashboardFiltroRequest filtro);

    @Query("""
    SELECT new br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardRankingProtocoloRow(
        u.id, u.nome, COUNT(p), AVG(CAST(FUNCTION('TIMESTAMPDIFF', MINUTE, p.dataCriacao, p.dataEncerramento) AS double)))
    FROM Protocolo p JOIN p.admin u
    WHERE p.status = br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo.FECHADO
      AND p.dataCriacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND (:#{#filtro.userIds.size()} = 0 OR u.id IN :#{#filtro.userIds})
    GROUP BY u.id, u.nome
    """)
    List<DashboardRankingProtocoloRow> rankingProtocolosPorUsuario(@Param("filtro") DashboardFiltroRequest filtro);

    /**
     * Nativa (ver comentário de classe/DASHBOARD.md §5) — por isso o filtro de usuário é feito
     * com uma flag booleana + lista (nunca vazia) em vez de "IN :usuarioIds" direto: um parâmetro
     * de coleção vazio gera "IN ()" em SQL nativo, que é erro de sintaxe no MySQL. Quando não há
     * restrição de usuário, o service passa semRestricaoUsuario=true e uma lista dummy não-vazia
     * (nunca avaliada, por causa do curto-circuito do OR).
     */
    @Query(value = """
    SELECT DATE(data_criacao) AS dia, COUNT(*) AS quantidade
    FROM protocolo
    WHERE data_criacao BETWEEN :inicio AND :fim
      AND (:semRestricaoUsuario = TRUE OR admin_id IN (:usuarioIds))
    GROUP BY DATE(data_criacao)
    """, nativeQuery = true)
    List<DiaContagemProjection> countAbertosPorDia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
                                                     @Param("semRestricaoUsuario") boolean semRestricaoUsuario,
                                                     @Param("usuarioIds") List<Long> usuarioIds);

    @Query(value = """
    SELECT DATE(data_encerramento) AS dia, COUNT(*) AS quantidade
    FROM protocolo
    WHERE status = 'FECHADO'
      AND data_encerramento BETWEEN :inicio AND :fim
      AND (:semRestricaoUsuario = TRUE OR admin_id IN (:usuarioIds))
    GROUP BY DATE(data_encerramento)
    """, nativeQuery = true)
    List<DiaContagemProjection> countFechadosPorDia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
                                                      @Param("semRestricaoUsuario") boolean semRestricaoUsuario,
                                                      @Param("usuarioIds") List<Long> usuarioIds);

    interface DiaContagemProjection {
        LocalDate getDia();
        Long getQuantidade();
    }
}

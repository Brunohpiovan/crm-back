package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardFiltroRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardRankingProtocoloRow;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
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

    @Query("SELECT p FROM Protocolo p JOIN FETCH p.admin JOIN FETCH p.participante LEFT JOIN FETCH p.adminAnterior WHERE (p.participante.celular = :celular) AND p.status = :status")
    Optional<Protocolo> findByCelularAndStatus(@Param("celular") String celular, @Param("status") StatusProtocolo status);

    @Query("SELECT p FROM Protocolo p JOIN FETCH p.admin JOIN FETCH p.participante LEFT JOIN FETCH p.adminAnterior WHERE p.participante.id = :usuarioId AND p.status = :status")
    Optional<Protocolo> findByParticipanteIdAndStatusAberto(@Param("usuarioId") Long usuarioId, @Param("status") StatusProtocolo status);


    @Query("""
    SELECT COUNT(p) FROM Protocolo p
    WHERE p.status = br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo.ABERTO
      AND p.dataCriacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND (:#{#filtro.userId} IS NULL OR p.admin.id = :#{#filtro.userId})
    """)
    long countAbertos(@Param("filtro") DashboardFiltroRequest filtro);

    @Query("""
    SELECT COUNT(p) FROM Protocolo p
    WHERE p.status = br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo.ABERTO
      AND p.dataCriacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND p.dataCriacao <= :limiteRisco
      AND (:#{#filtro.userId} IS NULL OR p.admin.id = :#{#filtro.userId})
    """)
    long countEmRisco(@Param("filtro") DashboardFiltroRequest filtro, @Param("limiteRisco") LocalDateTime limiteRisco);

    @Query("""
    SELECT AVG(FUNCTION('TIMESTAMPDIFF', MINUTE, p.dataCriacao, p.dataEncerramento)) FROM Protocolo p
    WHERE p.status = br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo.FECHADO
      AND p.dataCriacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND (:#{#filtro.userId} IS NULL OR p.admin.id = :#{#filtro.userId})
    """)
    Double avgTempoAtendimentoMinutos(@Param("filtro") DashboardFiltroRequest filtro);

    @Query("""
    SELECT new br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardRankingProtocoloRow(
        u.id, u.nome, COUNT(p), AVG(FUNCTION('TIMESTAMPDIFF', MINUTE, p.dataCriacao, p.dataEncerramento)))
    FROM Protocolo p JOIN p.admin u
    WHERE p.status = br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo.FECHADO
      AND p.dataCriacao BETWEEN :#{#filtro.startDate} AND :#{#filtro.endDate}
      AND (:#{#filtro.userId} IS NULL OR u.id = :#{#filtro.userId})
    GROUP BY u.id, u.nome
    """)
    List<DashboardRankingProtocoloRow> rankingProtocolosPorUsuario(@Param("filtro") DashboardFiltroRequest filtro);

    @Query(value = """
    SELECT DATE(data_criacao) AS dia, COUNT(*) AS quantidade
    FROM protocolo
    WHERE data_criacao BETWEEN :inicio AND :fim
      AND (:usuarioId IS NULL OR admin_id = :usuarioId)
    GROUP BY DATE(data_criacao)
    """, nativeQuery = true)
    List<DiaContagemProjection> countAbertosPorDia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("usuarioId") Long usuarioId);

    @Query(value = """
    SELECT DATE(data_encerramento) AS dia, COUNT(*) AS quantidade
    FROM protocolo
    WHERE status = 'FECHADO'
      AND data_encerramento BETWEEN :inicio AND :fim
      AND (:usuarioId IS NULL OR admin_id = :usuarioId)
    GROUP BY DATE(data_encerramento)
    """, nativeQuery = true)
    List<DiaContagemProjection> countFechadosPorDia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim, @Param("usuarioId") Long usuarioId);

    interface DiaContagemProjection {
        LocalDate getDia();
        Long getQuantidade();
    }
}

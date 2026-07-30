package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    Optional<List<Protocolo>> findByAdminLoginOrParticipanteLogin(@Param("login") String login);

    @Query("SELECT p FROM Protocolo p JOIN FETCH p.admin JOIN FETCH p.participante LEFT JOIN FETCH p.adminAnterior WHERE (p.participante.celular = :celular) AND p.status = :status")
    Optional<Protocolo> findByCelularAndStatus(@Param("celular") String celular, @Param("status") StatusProtocolo status);

    @Query("SELECT p FROM Protocolo p JOIN FETCH p.admin JOIN FETCH p.participante LEFT JOIN FETCH p.adminAnterior WHERE p.participante.id = :usuarioId AND p.status = :status")
    Optional<Protocolo> findByParticipanteIdAndStatusAberto(@Param("usuarioId") Long usuarioId, @Param("status") StatusProtocolo status);
}

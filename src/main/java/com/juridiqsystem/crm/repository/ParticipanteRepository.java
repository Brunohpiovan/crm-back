package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Participante;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.enums.StatusProtocolo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipanteRepository extends JpaRepository<Participante, Long> {
    Optional<Participante> findByPublicId(String publicId);

    Optional<Participante> findByCelular(String celular);
    Optional<Participante> findByLogin(String login);

    @Query("""
    SELECT p FROM Participante p
    WHERE p.id NOT IN (
        SELECT pr.participante.id FROM Protocolo pr
        WHERE pr.status = com.juridiqsystem.crm.model.enums.StatusProtocolo.ABERTO
        AND pr.admin.id <> :adminId
    )
""")
    List<Participante> findAllWithoutOpenProtocoloFromOtherAdmins(@Param("adminId") Long adminId);




}

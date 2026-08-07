package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Mensagem;
import com.juridiqsystem.crm.model.Protocolo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    Optional<Mensagem> findByPublicId(String publicId);

    @Query(value = "SELECT m FROM Mensagem m JOIN FETCH m.sender JOIN FETCH m.protocolo WHERE m.protocolo = :protocolo",
           countQuery = "SELECT COUNT(m) FROM Mensagem m WHERE m.protocolo = :protocolo")
    Page<Mensagem> findByProtocolo(@Param("protocolo") Protocolo protocolo, Pageable pageable);

    @Query("SELECT m FROM Mensagem m JOIN FETCH m.sender JOIN FETCH m.protocolo WHERE m.protocolo = :protocolo")
    List<Mensagem> findByProtocolo(@Param("protocolo") Protocolo protocolo);

    @Query("SELECT m FROM Mensagem m JOIN FETCH m.sender WHERE m.sender.id = :senderId AND m.protocolo IS NULL")
    List<Mensagem> findBySenderIdAndProtocoloIsNull(@Param("senderId") Long senderId);

    @Query("SELECT DISTINCT m.sender.publicId FROM Mensagem m WHERE m.protocolo IS NULL")
    List<String> findDistinctSenderIdsWithoutProtocol();
}

package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Mensagem;
import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    @Query(value = "SELECT m FROM Mensagem m JOIN FETCH m.sender JOIN FETCH m.protocolo WHERE m.protocolo = :protocolo",
           countQuery = "SELECT COUNT(m) FROM Mensagem m WHERE m.protocolo = :protocolo")
    Page<Mensagem> findByProtocolo(@Param("protocolo") Protocolo protocolo, Pageable pageable);

    @Query("SELECT m FROM Mensagem m JOIN FETCH m.sender JOIN FETCH m.protocolo WHERE m.protocolo = :protocolo")
    List<Mensagem> findByProtocolo(@Param("protocolo") Protocolo protocolo);

    @Query("SELECT m FROM Mensagem m JOIN FETCH m.sender WHERE m.sender.id = :senderId AND m.protocolo IS NULL")
    List<Mensagem> findBySenderIdAndProtocoloIsNull(@Param("senderId") Long senderId);

    @Query("SELECT DISTINCT m.sender.id FROM Mensagem m WHERE m.protocolo IS NULL")
    List<Long> findDistinctSenderIdsWithoutProtocol();
}

package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Mensagem;
import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    Page<Mensagem> findByProtocolo(Protocolo protocolo, Pageable pageable);
    List<Mensagem> findByProtocolo(Protocolo protocolo);
    @Query("SELECT m FROM Mensagem m WHERE m.sender.id = :userId AND m.protocolo IS NULL")
    List<Mensagem> findMessagesWithoutProtocol(@Param("userId") Long userId);
    List<Mensagem> findBySenderIdAndProtocoloIsNull(Long senderId);

    @Query("SELECT DISTINCT m.sender.id FROM Mensagem m WHERE m.protocolo IS NULL")
    List<Long> findDistinctSenderIdsWithoutProtocol();




}

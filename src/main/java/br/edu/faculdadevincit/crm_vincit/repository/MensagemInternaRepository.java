package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.ChatGrupo;
import br.edu.faculdadevincit.crm_vincit.model.MensagemInterna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MensagemInternaRepository extends JpaRepository<MensagemInterna, Long> {

    @Query(value = "SELECT m FROM MensagemInterna m JOIN FETCH m.sender WHERE m.chatGrupo = :chatGrupo",
           countQuery = "SELECT COUNT(m) FROM MensagemInterna m WHERE m.chatGrupo = :chatGrupo")
    Page<MensagemInterna> findByChatGrupo(@Param("chatGrupo") ChatGrupo chatGrupo, Pageable pageable);

}

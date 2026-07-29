package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.ChatGrupo;
import br.edu.faculdadevincit.crm_vincit.model.MensagemInterna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MensagemInternaRepository extends JpaRepository<MensagemInterna, Long> {
    Page<MensagemInterna> findByChatGrupo(ChatGrupo chatGrupo, Pageable pageable);

}

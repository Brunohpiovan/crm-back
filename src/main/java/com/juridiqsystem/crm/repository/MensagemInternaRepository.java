package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.ChatGrupo;
import com.juridiqsystem.crm.model.MensagemInterna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MensagemInternaRepository extends JpaRepository<MensagemInterna, Long> {

    Optional<MensagemInterna> findByPublicId(String publicId);

    @Query(value = "SELECT m FROM MensagemInterna m JOIN FETCH m.sender WHERE m.chatGrupo = :chatGrupo",
           countQuery = "SELECT COUNT(m) FROM MensagemInterna m WHERE m.chatGrupo = :chatGrupo")
    Page<MensagemInterna> findByChatGrupo(@Param("chatGrupo") ChatGrupo chatGrupo, Pageable pageable);

    /**
     * Última mensagem (por dataEnvio) de cada grupo em chatGrupoIds — batched, não N+1. Usado
     * tanto pelos contatos ativos (via grupo privado) quanto pelos grupos públicos.
     */
    @Query("""
    SELECT m FROM MensagemInterna m JOIN FETCH m.sender
    WHERE m.chatGrupo.id IN :chatGrupoIds
      AND m.dataEnvio = (
        SELECT MAX(m2.dataEnvio) FROM MensagemInterna m2 WHERE m2.chatGrupo.id = m.chatGrupo.id
      )
    """)
    List<MensagemInterna> findUltimasMensagensPorGrupos(@Param("chatGrupoIds") List<Long> chatGrupoIds);

}

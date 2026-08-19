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
     * Última mensagem de cada grupo em chatGrupoIds — batched, não N+1. Usado tanto pelos
     * contatos ativos (via grupo privado) quanto pelos grupos públicos. Desempate por MAX(id)
     * em vez de MAX(dataEnvio): duas mensagens do mesmo grupo podem ter o mesmo timestamp
     * (resolução do relógio), o que fazia essa query devolver 2 linhas para o mesmo grupo e
     * quebrava o Collectors.toMap chamador com "Duplicate key". O id é IDENTITY (sequencial e
     * único), então nunca empata.
     */
    @Query("""
    SELECT m FROM MensagemInterna m JOIN FETCH m.sender
    WHERE m.id IN (
      SELECT MAX(m2.id) FROM MensagemInterna m2
      WHERE m2.chatGrupo.id IN :chatGrupoIds
      GROUP BY m2.chatGrupo.id
    )
    """)
    List<MensagemInterna> findUltimasMensagensPorGrupos(@Param("chatGrupoIds") List<Long> chatGrupoIds);

}

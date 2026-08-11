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

    /**
     * Última mensagem de cada protocolo em protocoloIds — batched, não N+1. Desempate por
     * MAX(id) em vez de MAX(data_envio): duas mensagens do mesmo protocolo podem ter o mesmo
     * timestamp (ex.: sendMessage salvando conteúdo+mídia em sequência rápida), o que fazia
     * essa query devolver 2 linhas para o mesmo protocolo e quebrava o Collectors.toMap
     * chamador com "Duplicate key" (mesmo padrão já corrigido em
     * MensagemInternaRepository#findUltimasMensagensPorGrupos). m.getProtocolo().getId() no
     * lado do service não dispara select extra: protocolo é ManyToOne LAZY, mas o id fica
     * disponível no proxy sem inicializar a entidade.
     */
    @Query("""
    SELECT m FROM Mensagem m JOIN FETCH m.sender
    WHERE m.id IN (
      SELECT MAX(m2.id) FROM Mensagem m2
      WHERE m2.protocolo.id IN :protocoloIds
      GROUP BY m2.protocolo.id
    )
    """)
    List<Mensagem> findUltimasMensagensPorProtocolos(@Param("protocoloIds") List<Long> protocoloIds);

    /**
     * Última mensagem "pública" (m.protocolo IS NULL, contato ainda não atendido) de cada
     * participante em senderIds — batched, não N+1. Mesmo desempate por MAX(id) usado em
     * findUltimasMensagensPorProtocolos, pelo mesmo motivo (timestamps podem colidir).
     */
    @Query("""
    SELECT m FROM Mensagem m JOIN FETCH m.sender
    WHERE m.protocolo IS NULL
      AND m.sender.id IN :senderIds
      AND m.id IN (
        SELECT MAX(m2.id) FROM Mensagem m2
        WHERE m2.protocolo IS NULL AND m2.sender.id IN :senderIds
        GROUP BY m2.sender.id
      )
    """)
    List<Mensagem> findUltimasMensagensPublicasPorSenders(@Param("senderIds") List<Long> senderIds);
}

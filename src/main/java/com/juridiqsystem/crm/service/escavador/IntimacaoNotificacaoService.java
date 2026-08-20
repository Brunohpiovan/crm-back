package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.model.dtos.NovaIntimacaoNotificacaoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Avisa o frontend, em tempo real, que uma nova intimação/publicação foi detectada — canal geral
 * da empresa, mirror exato de NovoDocumentoNotificacaoService. O usuário pode não estar na página
 * de Intimações quando o callback de monitoramento chega.
 */
@Slf4j
@Service
public class IntimacaoNotificacaoService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /** AFTER_COMMIT: sem isso, o frontend recarregaria a lista antes da intimação estar salva. */
    @TransactionalEventListener(fallbackExecution = true)
    public void notificarNovaIntimacao(NovaIntimacaoDetectadaEvent evento) {
        NovaIntimacaoNotificacaoDTO notificacao = new NovaIntimacaoNotificacaoDTO(
                evento.intimacaoId(), evento.oabNumero(), evento.diarioNome(), LocalDateTime.now());
        String destino = "/topic/empresa/" + evento.empresaId() + "/intimacao";
        try {
            messagingTemplate.convertAndSend(destino, notificacao);
        } catch (RuntimeException e) {
            // Best-effort: perder o aviso não desfaz a intimação já salva (o usuário ainda a vê ao
            // reabrir a página de Intimações).
            log.error("Falha ao publicar notificacao de intimacao no topico {}.", destino, e);
        }
    }
}

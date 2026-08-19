package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.model.dtos.NovoDocumentoNotificacaoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Avisa o frontend, em tempo real, que um documento novo foi detectado num processo — canal geral
 * da empresa (não por processo), mesmo padrão de ResumoIaNotificacaoService: o usuário pode não
 * estar na tela daquele processo quando o callback de monitoramento chega.
 */
@Slf4j
@Service
public class NovoDocumentoNotificacaoService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /** AFTER_COMMIT: sem isso, o frontend recarregaria a lista antes do documento estar salvo. */
    @TransactionalEventListener(fallbackExecution = true)
    public void notificarNovoDocumento(NovoDocumentoDetectadoEvent evento) {
        NovoDocumentoNotificacaoDTO notificacao = new NovoDocumentoNotificacaoDTO(
                evento.processoPublicId(), evento.tituloDocumento(), LocalDateTime.now());
        String destino = "/topic/empresa/" + evento.empresaId() + "/documento";
        try {
            messagingTemplate.convertAndSend(destino, notificacao);
        } catch (RuntimeException e) {
            // Best-effort: perder o aviso não desfaz o documento já salvo (o usuário ainda o vê ao
            // reabrir a tela do processo).
            log.error("Falha ao publicar notificacao de documento no topico {}.", destino, e);
        }
    }
}

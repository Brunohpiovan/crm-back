package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.model.dtos.ResumoIaNotificacaoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Avisa o frontend, em tempo real, que um resumo por IA terminou de ser gerado — mesmo padrão de
 * ProcessoNotificacaoService, mas em canal geral da empresa (não por processo): o usuário pode ter
 * saído da tela do processo enquanto a geração (assíncrona, até ~2min) rodava em background.
 */
@Slf4j
@Service
public class ResumoIaNotificacaoService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /** AFTER_COMMIT: sem isso, o frontend recarregaria o processo antes do resumoIa estar salvo. */
    @TransactionalEventListener(fallbackExecution = true)
    public void notificarResumoConcluido(ResumoIaConcluidoEvent evento) {
        ResumoIaNotificacaoDTO notificacao = new ResumoIaNotificacaoDTO(
                evento.processoPublicId(), evento.numeroCnj(), LocalDateTime.now());
        String destino = "/topic/empresa/" + evento.empresaId() + "/resumo-ia";
        try {
            messagingTemplate.convertAndSend(destino, notificacao);
        } catch (RuntimeException e) {
            // Best-effort: perder o aviso não desfaz o resumo já salvo (o usuário ainda o vê ao
            // reabrir a tela do processo).
            log.error("Falha ao publicar notificacao de resumo IA no topico {}.", destino, e);
        }
    }
}

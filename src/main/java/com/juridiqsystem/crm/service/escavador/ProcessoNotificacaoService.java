package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.model.dtos.ProcessoMovimentacaoNotificacaoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

/**
 * Avisa o frontend, em tempo real, que um processo aberto na tela ganhou movimentações novas —
 * venham elas de callback da Escavador ou de atualização manual (o evento de origem é o mesmo).
 *
 * <p>Mesmo padrão de tópico de ProtocoloService: "/topic/empresa/{empresaId}/..." é escopado por
 * empresa e o StompAuthChannelInterceptor já recusa assinatura de tópico de outra empresa, então
 * nenhuma autorização adicional é necessária aqui.</p>
 */
@Slf4j
@Service
public class ProcessoNotificacaoService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * AFTER_COMMIT (mesma intenção do publishAfterCommit de ProtocoloService): sem isso, o
     * frontend receberia o aviso, refaria o fetch da timeline e não encontraria as movimentações
     * que ainda não foram commitadas.
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void notificarNovasMovimentacoes(NovasMovimentacoesDetectadasEvent evento) {
        ProcessoMovimentacaoNotificacaoDTO notificacao = new ProcessoMovimentacaoNotificacaoDTO(
                evento.processoPublicId(), evento.movimentacaoIds().size(), LocalDateTime.now());
        String destino = "/topic/empresa/" + evento.empresaId()
                + "/processo/" + evento.processoPublicId() + "/movimentacao";
        try {
            messagingTemplate.convertAndSend(destino, notificacao);
        } catch (RuntimeException e) {
            // Notificação é entrega best-effort: perder o aviso não pode desfazer movimentações
            // já gravadas (o usuário ainda as vê ao recarregar a tela).
            log.error("Falha ao publicar notificacao de movimentacao no topico {}.", destino, e);
        }
    }
}

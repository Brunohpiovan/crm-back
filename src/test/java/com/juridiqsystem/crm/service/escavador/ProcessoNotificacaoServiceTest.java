package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.model.dtos.ProcessoMovimentacaoNotificacaoDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * O tópico precisa bater exatamente com o que o frontend assina
 * (useProcessoMovimentacaoSocket.ts) e com o prefixo "/topic/empresa/{empresaId}/" que o
 * StompAuthChannelInterceptor usa para impedir que um usuário assine tópico de outra empresa —
 * errar o formato aqui vazaria o aviso para fora do tenant ou simplesmente não entregaria nada.
 */
@ExtendWith(MockitoExtension.class)
class ProcessoNotificacaoServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ProcessoNotificacaoService processoNotificacaoService;

    @Test
    void notificarNovasMovimentacoes_publicaNoTopicoDaEmpresaEDoProcesso() {
        processoNotificacaoService.notificarNovasMovimentacoes(
                new NovasMovimentacoesDetectadasEvent(10L, "processo-abc", 7L, List.of(1L, 2L, 3L)));

        ArgumentCaptor<String> destino = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(destino.capture(), payload.capture());

        assertThat(destino.getValue()).isEqualTo("/topic/empresa/7/processo/processo-abc/movimentacao");
        ProcessoMovimentacaoNotificacaoDTO dto = (ProcessoMovimentacaoNotificacaoDTO) payload.getValue();
        assertThat(dto.processoPublicId()).isEqualTo("processo-abc");
        assertThat(dto.quantidade()).isEqualTo(3);
        assertThat(dto.ocorridoEm()).isNotNull();
    }
}

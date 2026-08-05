package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.WhatsappWebhookEvento;
import br.edu.faculdadevincit.crm_vincit.repository.WhatsappWebhookEventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre o item crítico C6 da auditoria: o MessageSid do webhook Twilio precisa ser registrado
 * ANTES do processamento pesado (não depois), para que um reenvio concorrente do mesmo webhook
 * (comum quando o processamento anterior demora e estoura o timeout da Twilio) esbarre na
 * constraint única em vez de reprocessar tudo em paralelo. E se o processamento falhar depois de
 * registrado, o registro precisa ser desfeito para não perder a mensagem permanentemente.
 */
@ExtendWith(MockitoExtension.class)
class WhatsAppServiceIdempotenciaTest {

    @Mock
    private WhatsappWebhookEventoRepository whatsappWebhookEventoRepository;

    @InjectMocks
    private WhatsAppService whatsAppService;

    @Test
    void tentarRegistrar_messageSidNovo_salvaERetornaTrue() {
        when(whatsappWebhookEventoRepository.existsByMessageSid("SID1")).thenReturn(false);

        boolean registrado = ReflectionTestUtils.invokeMethod(
                whatsAppService, "tentarRegistrarMensagemComoProcessada", "SID1");

        assertThat(registrado).isTrue();
        verify(whatsappWebhookEventoRepository).save(any(WhatsappWebhookEvento.class));
    }

    @Test
    void tentarRegistrar_messageSidJaExistente_naoSalvaERetornaFalse() {
        when(whatsappWebhookEventoRepository.existsByMessageSid("SID1")).thenReturn(true);

        boolean registrado = ReflectionTestUtils.invokeMethod(
                whatsAppService, "tentarRegistrarMensagemComoProcessada", "SID1");

        assertThat(registrado).isFalse();
        verify(whatsappWebhookEventoRepository, never()).save(any());
    }

    @Test
    void tentarRegistrar_corridaConcorrente_constraintUnicaRetornaFalse() {
        when(whatsappWebhookEventoRepository.existsByMessageSid("SID1")).thenReturn(false);
        when(whatsappWebhookEventoRepository.save(any(WhatsappWebhookEvento.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        boolean registrado = ReflectionTestUtils.invokeMethod(
                whatsAppService, "tentarRegistrarMensagemComoProcessada", "SID1");

        assertThat(registrado).isFalse();
    }

    @Test
    void desfazerRegistro_removeMessageSid() {
        ReflectionTestUtils.invokeMethod(whatsAppService, "desfazerRegistroDeProcessamento", "SID1");

        verify(whatsappWebhookEventoRepository).deleteByMessageSid(eq("SID1"));
    }

    @Test
    void desfazerRegistro_messageSidVazio_naoChamaRepositorio() {
        ReflectionTestUtils.invokeMethod(whatsAppService, "desfazerRegistroDeProcessamento", "");

        verify(whatsappWebhookEventoRepository, never()).deleteByMessageSid(any());
    }
}

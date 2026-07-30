package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.service.exceptions.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * POST /whatsapp/webhook é permitAll no SecurityConfiguration: a única defesa contra mensagens e
 * participantes forjados é a verificação de assinatura Twilio dentro de WhatsAppService. Este teste
 * garante que uma assinatura ausente ou inválida é sempre rejeitada antes de qualquer efeito colateral
 * (criação de participante/protocolo/mensagem).
 */
@ExtendWith(MockitoExtension.class)
class WhatsAppServiceWebhookTest {

    private WhatsAppService whatsAppService;

    @BeforeEach
    void setUp() {
        whatsAppService = new WhatsAppService();
        ReflectionTestUtils.setField(whatsAppService, "authToken", "auth-token-de-teste");
    }

    @Test
    void receiveRequest_semHeaderDeAssinatura_lancaAccessDenied() {
        Map<String, String> params = Map.of("From", "whatsapp:+5511999999999", "Body", "Olá");

        assertThatThrownBy(() -> whatsAppService.receiveRequest(params, "https://api.exemplo.com/whatsapp/webhook", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Assinatura Twilio inválida");
    }

    @Test
    void receiveRequest_headerDeAssinaturaEmBranco_lancaAccessDenied() {
        Map<String, String> params = Map.of("From", "whatsapp:+5511999999999", "Body", "Olá");

        assertThatThrownBy(() -> whatsAppService.receiveRequest(params, "https://api.exemplo.com/whatsapp/webhook", "   "))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Assinatura Twilio inválida");
    }

    @Test
    void receiveRequest_assinaturaInvalida_lancaAccessDenied() {
        Map<String, String> params = Map.of("From", "whatsapp:+5511999999999", "Body", "Olá");

        assertThatThrownBy(() -> whatsAppService.receiveRequest(params, "https://api.exemplo.com/whatsapp/webhook", "assinatura-forjada-obviamente-errada"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Assinatura Twilio inválida");
    }
}

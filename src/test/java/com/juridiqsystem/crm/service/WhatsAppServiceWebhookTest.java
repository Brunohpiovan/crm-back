package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.whatsapp.MetaWebhookMapper;
import com.juridiqsystem.crm.infra.whatsapp.MetaWebhookSignatureValidator;
import com.juridiqsystem.crm.infra.whatsapp.MetaWhatsAppClient;
import com.juridiqsystem.crm.infra.whatsapp.MetaWhatsAppProperties;
import com.juridiqsystem.crm.model.Mensagem;
import com.juridiqsystem.crm.model.Participante;
import com.juridiqsystem.crm.model.WhatsAppIntegration;
import com.juridiqsystem.crm.model.WhatsappWebhookEvento;
import com.juridiqsystem.crm.model.enums.WhatsAppIntegrationStatus;
import com.juridiqsystem.crm.model.whatsapp.IncomingWhatsAppMessage;
import com.juridiqsystem.crm.model.whatsapp.MessageType;
import com.juridiqsystem.crm.model.whatsapp.WhatsAppWebhookPayload;
import com.juridiqsystem.crm.repository.MensagemRepository;
import com.juridiqsystem.crm.repository.ParticipanteRepository;
import com.juridiqsystem.crm.repository.ProtocoloRepository;
import com.juridiqsystem.crm.repository.WhatsAppIntegrationRepository;
import com.juridiqsystem.crm.repository.WhatsappWebhookEventoRepository;
import com.juridiqsystem.crm.service.exceptions.MetaWebhookException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre o núcleo do recebimento de webhook Meta: validação de assinatura ANTES de qualquer coisa
 * (diferente do modelo Twilio, aqui não depende de resolver tenant primeiro), resolução de tenant
 * por phone_number_id, e idempotência por external_message_id (substitui WhatsAppServiceWebhookTest/
 * WhatsAppServiceIdempotenciaTest do fluxo Twilio).
 */
@ExtendWith(MockitoExtension.class)
class WhatsAppServiceWebhookTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ProtocoloRepository protocoloRepository;
    @Mock
    private MensagemService mensagemService;
    @Mock
    private MensagemRepository mensagemRepository;
    @Mock
    private ParticipanteRepository participanteRepository;
    @Mock
    private S3Service s3Service;
    @Mock
    private com.juridiqsystem.crm.repository.OportunidadeRepository oportunidadeRepository;
    @Mock
    private WhatsappWebhookEventoRepository whatsappWebhookEventoRepository;
    @Mock
    private WhatsAppIntegrationRepository whatsAppIntegrationRepository;
    @Mock
    private MetaWhatsAppClient metaWhatsAppClient;
    @Mock
    private MetaWebhookMapper metaWebhookMapper;
    @Mock
    private MetaWebhookSignatureValidator signatureValidator;
    @Mock
    private MetaWhatsAppProperties metaProperties;
    @Mock
    private com.juridiqsystem.crm.service.auth.SlidingWindowRateLimiter rateLimiter;

    @InjectMocks
    private WhatsAppService whatsAppService;

    private static final String RAW_BODY = "{\"object\":\"whatsapp_business_account\"}";
    private static final String SIGNATURE = "sha256=abcdef";
    private static final String APP_SECRET = "app-secret";

    @Test
    void assinaturaInvalidaRejeitaAntesDeInterpretarPayload() {
        when(metaProperties.getAppSecret()).thenReturn(APP_SECRET);
        when(signatureValidator.isValid(RAW_BODY, SIGNATURE, APP_SECRET)).thenReturn(false);

        assertThatThrownBy(() -> whatsAppService.receiveWebhookEvent(RAW_BODY, SIGNATURE))
                .isInstanceOf(MetaWebhookException.class);

        verify(metaWebhookMapper, never()).parse(anyString());
    }

    @Test
    void phoneNumberIdDesconhecidoNaoProcessaNadaNemLancaExcecao() {
        when(metaProperties.getAppSecret()).thenReturn(APP_SECRET);
        when(signatureValidator.isValid(RAW_BODY, SIGNATURE, APP_SECRET)).thenReturn(true);
        when(metaWebhookMapper.parse(RAW_BODY)).thenReturn(new WhatsAppWebhookPayload("999999", List.of(), List.of()));
        when(whatsAppIntegrationRepository.findByPhoneNumberIdIgnoringTenant("999999")).thenReturn(Optional.empty());

        whatsAppService.receiveWebhookEvent(RAW_BODY, SIGNATURE);

        verify(mensagemService, never()).sendMessagePublico(any(), any(), any(), any());
    }

    @Test
    void mensagemNovaSemProtocoloAbertoEhPersistidaEIdempotenciaEhRegistrada() {
        WhatsAppIntegration integration = new WhatsAppIntegration();
        integration.setEmpresaId(42L);
        integration.setPhoneNumberId("1234567890");
        integration.setStatus(WhatsAppIntegrationStatus.CONNECTED);

        IncomingWhatsAppMessage incoming = new IncomingWhatsAppMessage(
                "wamid.NOVO", "1234567890", "5511987654321", MessageType.TEXT, "Olá", null, null, "Cliente", Instant.now());

        when(metaProperties.getAppSecret()).thenReturn(APP_SECRET);
        when(signatureValidator.isValid(RAW_BODY, SIGNATURE, APP_SECRET)).thenReturn(true);
        when(metaWebhookMapper.parse(RAW_BODY)).thenReturn(new WhatsAppWebhookPayload("1234567890", List.of(incoming), List.of()));
        when(whatsAppIntegrationRepository.findByPhoneNumberIdIgnoringTenant("1234567890")).thenReturn(Optional.of(integration));
        when(whatsappWebhookEventoRepository.existsByExternalMessageId("wamid.NOVO")).thenReturn(false);

        Participante participante = new Participante();
        participante.setCelular("11987654321");
        when(participanteRepository.findByCelular("11987654321")).thenReturn(Optional.of(participante));
        when(protocoloRepository.findByCelularAndStatus(anyString(), any())).thenReturn(Optional.empty());
        Mensagem mensagemSalva = new Mensagem();
        mensagemSalva.setSender(participante);
        when(mensagemService.sendMessagePublico(any(), any(), any(), anyString())).thenReturn(List.of(mensagemSalva));

        whatsAppService.receiveWebhookEvent(RAW_BODY, SIGNATURE);

        verify(whatsappWebhookEventoRepository, times(1)).save(any(WhatsappWebhookEvento.class));
        verify(mensagemService).sendMessagePublico(participante, "Olá", null, "wamid.NOVO");
    }

    @Test
    void mensagemJaProcessadaEhIgnoradaSilenciosamente() {
        WhatsAppIntegration integration = new WhatsAppIntegration();
        integration.setEmpresaId(42L);
        integration.setPhoneNumberId("1234567890");
        integration.setStatus(WhatsAppIntegrationStatus.CONNECTED);

        IncomingWhatsAppMessage incoming = new IncomingWhatsAppMessage(
                "wamid.REPETIDO", "1234567890", "5511987654321", MessageType.TEXT, "Olá de novo", null, null, "Cliente", Instant.now());

        when(metaProperties.getAppSecret()).thenReturn(APP_SECRET);
        when(signatureValidator.isValid(RAW_BODY, SIGNATURE, APP_SECRET)).thenReturn(true);
        when(metaWebhookMapper.parse(RAW_BODY)).thenReturn(new WhatsAppWebhookPayload("1234567890", List.of(incoming), List.of()));
        when(whatsAppIntegrationRepository.findByPhoneNumberIdIgnoringTenant("1234567890")).thenReturn(Optional.of(integration));
        when(whatsappWebhookEventoRepository.existsByExternalMessageId("wamid.REPETIDO")).thenReturn(true);

        whatsAppService.receiveWebhookEvent(RAW_BODY, SIGNATURE);

        verify(whatsappWebhookEventoRepository, never()).save(any());
        verify(participanteRepository, never()).findByCelular(anyString());
        verify(mensagemService, never()).sendMessagePublico(any(), any(), any(), any());
    }

    @Test
    void verifyWebhookRetornaChallengeQuandoTokenBate() {
        when(metaProperties.getVerifyToken()).thenReturn("meu-verify-token");

        String challenge = whatsAppService.verifyWebhook("subscribe", "meu-verify-token", "challenge-123");

        assertThat(challenge).isEqualTo("challenge-123");
    }

    @Test
    void verifyWebhookRejeitaTokenErrado() {
        when(metaProperties.getVerifyToken()).thenReturn("meu-verify-token");

        assertThatThrownBy(() -> whatsAppService.verifyWebhook("subscribe", "token-errado", "challenge-123"))
                .isInstanceOf(MetaWebhookException.class);
    }
}

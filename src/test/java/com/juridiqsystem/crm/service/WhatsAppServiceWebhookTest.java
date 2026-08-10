package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.EmpresaTwilioConfig;
import com.juridiqsystem.crm.repository.EmpresaTwilioConfigRepository;
import com.juridiqsystem.crm.repository.ParticipanteRepository;
import com.juridiqsystem.crm.repository.ProtocoloRepository;
import com.juridiqsystem.crm.repository.WhatsappWebhookEventoRepository;
import com.juridiqsystem.crm.service.exceptions.AccessDeniedException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * POST /whatsapp/webhook é permitAll no SecurityConfiguration: a única defesa contra mensagens e
 * participantes forjados é a verificação de assinatura Twilio dentro de WhatsAppService — e agora,
 * multi-tenant, essa defesa depende de resolver primeiro a EmpresaTwilioConfig certa a partir do
 * número "To" antes de validar a assinatura com o Auth Token daquela empresa especificamente
 * (nunca um Auth Token global). Este teste garante que:
 * - número de destino desconhecido/desativado é rejeitado antes de qualquer processamento;
 * - assinatura ausente/inválida/calculada com o token errado é sempre rejeitada;
 * - uma assinatura válida, calculada com o Auth Token da empresa certa, é aceita.
 */
@ExtendWith(MockitoExtension.class)
class WhatsAppServiceWebhookTest {

    private static final String REQUEST_URL = "https://api.exemplo.com/whatsapp/webhook";

    @Mock
    private EmpresaTwilioConfigRepository empresaTwilioConfigRepository;

    @Mock
    private ParticipanteRepository participanteRepository;

    @Mock
    private ProtocoloRepository protocoloRepository;

    @Mock
    private WhatsappWebhookEventoRepository whatsappWebhookEventoRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MensagemService mensagemService;

    @InjectMocks
    private WhatsAppService whatsAppService;

    @AfterEach
    void limpaTenantContext() {
        TenantContext.clear();
    }

    private EmpresaTwilioConfig criaConfig(Long empresaId, String whatsappNumber, String authToken, boolean enabled) {
        EmpresaTwilioConfig config = new EmpresaTwilioConfig();
        config.setId(empresaId);
        config.setEmpresaId(empresaId);
        config.setWhatsappNumber(whatsappNumber);
        config.setAuthToken(authToken);
        config.setAccountSid("AC" + empresaId);
        config.setEnabled(enabled);
        return config;
    }

    private Map<String, String> params(String to, String from, String body) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("To", to);
        params.put("From", from);
        params.put("Body", body);
        params.put("MessageSid", "SM_teste_" + to.hashCode());
        return params;
    }

    /** Reimplementação do algoritmo público de assinatura da Twilio, só para gerar assinaturas válidas em teste. */
    private String assinaturaValida(String url, Map<String, String> params, String authToken) {
        try {
            StringBuilder data = new StringBuilder(url);
            for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
                data.append(entry.getKey()).append(entry.getValue());
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(authToken.getBytes(), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.toString().getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void receiveRequest_numeroDestinoDesconhecido_lancaResourceNotFound_semValidarAssinatura() {
        Map<String, String> params = params("whatsapp:+10000000000", "whatsapp:+5511999999999", "Olá");
        when(empresaTwilioConfigRepository.findByWhatsappNumberIgnoringTenant("+10000000000"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> whatsAppService.receiveRequest(params, REQUEST_URL, "qualquer-coisa"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void receiveRequest_integracaoDesativada_lancaResourceNotFound() {
        Map<String, String> params = params("whatsapp:+10000000001", "whatsapp:+5511999999999", "Olá");
        EmpresaTwilioConfig config = criaConfig(1L, "+10000000001", "token-empresa-1", false);
        when(empresaTwilioConfigRepository.findByWhatsappNumberIgnoringTenant("+10000000001"))
                .thenReturn(Optional.of(config));

        assertThatThrownBy(() -> whatsAppService.receiveRequest(params, REQUEST_URL, "qualquer-coisa"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void receiveRequest_semHeaderDeAssinatura_lancaAccessDenied() {
        Map<String, String> params = params("whatsapp:+10000000002", "whatsapp:+5511999999999", "Olá");
        EmpresaTwilioConfig config = criaConfig(2L, "+10000000002", "token-empresa-2", true);
        when(empresaTwilioConfigRepository.findByWhatsappNumberIgnoringTenant("+10000000002"))
                .thenReturn(Optional.of(config));

        assertThatThrownBy(() -> whatsAppService.receiveRequest(params, REQUEST_URL, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Assinatura Twilio inválida");
    }

    @Test
    void receiveRequest_assinaturaCalculadaComTokenDeOutraEmpresa_lancaAccessDenied() {
        Map<String, String> params = params("whatsapp:+10000000003", "whatsapp:+5511999999999", "Olá");
        EmpresaTwilioConfig configDestino = criaConfig(3L, "+10000000003", "token-empresa-3", true);
        when(empresaTwilioConfigRepository.findByWhatsappNumberIgnoringTenant("+10000000003"))
                .thenReturn(Optional.of(configDestino));

        // Assinatura válida... mas calculada com o token de OUTRA empresa (nunca deve ser aceita
        // contra o número que pertence à empresa 3).
        String assinaturaComTokenErrado = assinaturaValida(REQUEST_URL, params, "token-empresa-999-diferente");

        assertThatThrownBy(() -> whatsAppService.receiveRequest(params, REQUEST_URL, assinaturaComTokenErrado))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void receiveRequest_assinaturaValidaComTokenDaEmpresaCerta_processaDentroDoTenantCorreto() {
        Long empresaId = 4L;
        String authToken = "token-empresa-4";
        Map<String, String> params = params("whatsapp:+10000000004", "whatsapp:+5511999999999", "Olá");
        EmpresaTwilioConfig config = criaConfig(empresaId, "+10000000004", authToken, true);
        when(empresaTwilioConfigRepository.findByWhatsappNumberIgnoringTenant("+10000000004"))
                .thenReturn(Optional.of(config));
        when(whatsappWebhookEventoRepository.existsByMessageSid(any())).thenReturn(false);
        when(participanteRepository.findByCelular(any())).thenAnswer(invocation -> {
            // Neste ponto o processamento já deve estar rodando dentro do tenant certo.
            assertThat(TenantContext.get()).isEqualTo(empresaId);
            return Optional.empty();
        });

        String assinatura = assinaturaValida(REQUEST_URL, params, authToken);

        whatsAppService.receiveRequest(params, REQUEST_URL, assinatura);

        // TenantContext.runAs sempre restaura o valor anterior (aqui, nenhum) ao final.
        assertThat(TenantContext.get()).isNull();
    }
}

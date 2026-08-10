package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.EmpresaTwilioConfig;
import com.juridiqsystem.crm.model.MensagemRequest;
import com.juridiqsystem.crm.repository.EmpresaTwilioConfigRepository;
import com.juridiqsystem.crm.service.auth.SlidingWindowRateLimiter;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Cobre o envio de mensagem (POST /whatsapp/send) no modelo multi-tenant: a empresa é sempre
 * resolvida a partir do TenantContext (populado pelo SecurityFilter a partir do JWT), nunca de um
 * parâmetro vindo do frontend, e o TwilioRestClient/número usado tem que ser exatamente o daquela
 * empresa — nunca de uma configuração global ou de outro tenant.
 */
@ExtendWith(MockitoExtension.class)
class WhatsAppServiceMultiTenantTest {

    @Mock
    private EmpresaTwilioConfigRepository empresaTwilioConfigRepository;

    @Mock
    private TwilioClientProvider twilioClientProvider;

    @Mock
    private SlidingWindowRateLimiter rateLimiter;

    @InjectMocks
    private WhatsAppService whatsAppService;

    @AfterEach
    void limpaContexto() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private void autenticaComo(String login) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
    }

    private EmpresaTwilioConfig config(Long empresaId, String whatsappNumber, boolean enabled) {
        EmpresaTwilioConfig config = new EmpresaTwilioConfig();
        config.setEmpresaId(empresaId);
        config.setAccountSid("AC_EMPRESA_" + empresaId);
        config.setAuthToken("token-empresa-" + empresaId);
        config.setWhatsappNumber(whatsappNumber);
        config.setEnabled(enabled);
        return config;
    }

    @Test
    void sendWhatsAppMessage_empresaSemIntegracaoConfigurada_lancaConflictSemChamarTwilio() {
        autenticaComo("user@empresa1.com");
        when(rateLimiter.tryAcquire(any(), anyInt(), any(Duration.class))).thenReturn(true);
        TenantContext.set(1L);
        when(empresaTwilioConfigRepository.findByEmpresaId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> whatsAppService.sendWhatsAppMessage(new MensagemRequest("11999999999", "oi", null)))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(twilioClientProvider);
    }

    @Test
    void sendWhatsAppMessage_semAutenticacaoNoSecurityContext_naoLancaNullPointer() {
        // Regressão: ChatService.sendMessage chama sendWhatsAppMessage a partir do fluxo STOMP
        // (/app/send), onde o SecurityContextHolder nunca é populado (só o Principal da própria
        // mensagem STOMP, via StompAuthChannelInterceptor). Antes do fix, isso lançava NPE aqui
        // mesmo, antes de qualquer validação de negócio.
        when(rateLimiter.tryAcquire(any(), anyInt(), any(Duration.class))).thenReturn(true);
        TenantContext.set(1L);
        when(empresaTwilioConfigRepository.findByEmpresaId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> whatsAppService.sendWhatsAppMessage(new MensagemRequest("11999999999", "oi", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void sendWhatsAppMessage_integracaoDesativada_lancaConflictSemChamarTwilio() {
        autenticaComo("user@empresa1.com");
        when(rateLimiter.tryAcquire(any(), anyInt(), any(Duration.class))).thenReturn(true);
        TenantContext.set(1L);
        when(empresaTwilioConfigRepository.findByEmpresaId(1L)).thenReturn(Optional.of(config(1L, "+10000000001", false)));

        assertThatThrownBy(() -> whatsAppService.sendWhatsAppMessage(new MensagemRequest("11999999999", "oi", null)))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(twilioClientProvider);
    }

    @Test
    void sendWhatsAppMessage_resolveClientENumeroExclusivamenteDaEmpresaAutenticada() {
        autenticaComo("user@empresa1.com");
        when(rateLimiter.tryAcquire(any(), anyInt(), any(Duration.class))).thenReturn(true);
        TenantContext.set(1L);
        EmpresaTwilioConfig configEmpresa1 = config(1L, "+10000000001", true);
        when(empresaTwilioConfigRepository.findByEmpresaId(1L)).thenReturn(Optional.of(configEmpresa1));
        // Interrompe o fluxo logo após resolver o client, antes de qualquer chamada de rede real à Twilio.
        when(twilioClientProvider.getClient(configEmpresa1)).thenThrow(new RuntimeException("stop-antes-da-chamada-de-rede"));

        assertThatThrownBy(() -> whatsAppService.sendWhatsAppMessage(new MensagemRequest("11999999999", "oi", null)))
                .hasMessage("stop-antes-da-chamada-de-rede");

        ArgumentCaptor<EmpresaTwilioConfig> captor = ArgumentCaptor.forClass(EmpresaTwilioConfig.class);
        verify(twilioClientProvider).getClient(captor.capture());
        assertThat(captor.getValue().getEmpresaId()).isEqualTo(1L);
        assertThat(captor.getValue().getWhatsappNumber()).isEqualTo("+10000000001");
        verify(empresaTwilioConfigRepository, never()).findByEmpresaId(eq(2L));
    }

    @Test
    void sendWhatsAppMessage_chamadasConcorrentesDeEmpresasDiferentes_nuncaTrocamCredenciais() throws InterruptedException {
        EmpresaTwilioConfig configEmpresa1 = config(1L, "+10000000001", true);
        EmpresaTwilioConfig configEmpresa2 = config(2L, "+10000000002", true);
        when(empresaTwilioConfigRepository.findByEmpresaId(1L)).thenReturn(Optional.of(configEmpresa1));
        when(empresaTwilioConfigRepository.findByEmpresaId(2L)).thenReturn(Optional.of(configEmpresa2));
        when(twilioClientProvider.getClient(any())).thenThrow(new RuntimeException("stop-antes-da-chamada-de-rede"));
        when(rateLimiter.tryAcquire(any(), anyInt(), any(Duration.class))).thenReturn(true);

        CountDownLatch largada = new CountDownLatch(1);
        AtomicReference<Long> tenantVistoPelaThreadA = new AtomicReference<>();
        AtomicReference<Long> tenantVistoPelaThreadB = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            pool.submit(() -> {
                aguarda(largada);
                autenticaComo("user@empresa1.com");
                TenantContext.set(1L);
                try {
                    whatsAppService.sendWhatsAppMessage(new MensagemRequest("11999999999", "oi", null));
                } catch (RuntimeException ignored) {
                    tenantVistoPelaThreadA.set(TenantContext.get());
                } finally {
                    TenantContext.clear();
                    SecurityContextHolder.clearContext();
                }
            });
            pool.submit(() -> {
                aguarda(largada);
                autenticaComo("user@empresa2.com");
                TenantContext.set(2L);
                try {
                    whatsAppService.sendWhatsAppMessage(new MensagemRequest("11999999999", "oi", null));
                } catch (RuntimeException ignored) {
                    tenantVistoPelaThreadB.set(TenantContext.get());
                } finally {
                    TenantContext.clear();
                    SecurityContextHolder.clearContext();
                }
            });

            largada.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        // TenantContext é ThreadLocal: cada thread tem que ter visto exclusivamente o próprio tenant,
        // nunca o da outra — mesmo com as duas rodando ao mesmo tempo.
        assertThat(tenantVistoPelaThreadA.get()).isEqualTo(1L);
        assertThat(tenantVistoPelaThreadB.get()).isEqualTo(2L);

        // E o client Twilio resolvido por cada chamada concorrente correspondeu exatamente à
        // config da própria empresa, nunca uma trocada pela outra thread.
        ArgumentCaptor<EmpresaTwilioConfig> captor = ArgumentCaptor.forClass(EmpresaTwilioConfig.class);
        verify(twilioClientProvider, org.mockito.Mockito.times(2)).getClient(captor.capture());
        List<Long> empresaIdsResolvidos = captor.getAllValues().stream().map(EmpresaTwilioConfig::getEmpresaId).sorted().toList();
        assertThat(empresaIdsResolvidos).containsExactly(1L, 2L);
    }

    private void aguarda(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.config.CacheConfig;
import com.juridiqsystem.crm.model.EmpresaTwilioConfig;
import com.twilio.http.TwilioRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Substitui o antigo bean singleton TwilioConfig (uma única conta Twilio global). Como múltiplas
 * empresas podem enviar mensagens simultaneamente, é crítico que cada uma sempre receba um
 * TwilioRestClient construído com as SUAS próprias credenciais — nunca um client de outra empresa
 * vazado por cache mal escopado ou por estado mutável compartilhado.
 */
class TwilioClientProviderTest {

    private TwilioClientProvider provider;

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = new ConcurrentMapCacheManager(CacheConfig.TWILIO_CLIENTS_CACHE);
        provider = new TwilioClientProvider(cacheManager);
    }

    private EmpresaTwilioConfig config(Long empresaId, String accountSid, String authToken) {
        EmpresaTwilioConfig config = new EmpresaTwilioConfig();
        config.setEmpresaId(empresaId);
        config.setAccountSid(accountSid);
        config.setAuthToken(authToken);
        config.setWhatsappNumber("+1000000000" + empresaId);
        config.setEnabled(true);
        return config;
    }

    @Test
    void getClient_empresasDiferentes_retornaClientsDistintos() {
        TwilioRestClient clienteA = provider.getClient(config(1L, "AC1", "token-1"));
        TwilioRestClient clienteB = provider.getClient(config(2L, "AC2", "token-2"));

        assertThat(clienteA).isNotSameAs(clienteB);
        assertThat(clienteA.getAccountSid()).isEqualTo("AC1");
        assertThat(clienteB.getAccountSid()).isEqualTo("AC2");
    }

    @Test
    void getClient_mesmaEmpresa_reutilizaClientDoCache() {
        EmpresaTwilioConfig config = config(1L, "AC1", "token-1");

        TwilioRestClient primeiraChamada = provider.getClient(config);
        TwilioRestClient segundaChamada = provider.getClient(config);

        assertThat(primeiraChamada).isSameAs(segundaChamada);
    }

    @Test
    void invalidate_forcaReconstrucaoComCredenciaisNovas() {
        Long empresaId = 1L;
        TwilioRestClient clienteAntigo = provider.getClient(config(empresaId, "AC_ANTIGO", "token-antigo"));

        provider.invalidate(empresaId);

        TwilioRestClient clienteNovo = provider.getClient(config(empresaId, "AC_NOVO", "token-novo"));

        assertThat(clienteNovo).isNotSameAs(clienteAntigo);
        assertThat(clienteNovo.getAccountSid()).isEqualTo("AC_NOVO");
    }

    @Test
    void getClient_chamadasConcorrentesDeEmpresasDiferentes_nuncaMisturamCredenciais() throws InterruptedException {
        int empresaAId = 1;
        int empresaBId = 2;
        CountDownLatch largada = new CountDownLatch(1);
        AtomicReference<TwilioRestClient> resultadoA = new AtomicReference<>();
        AtomicReference<TwilioRestClient> resultadoB = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            pool.submit(() -> {
                await(largada);
                resultadoA.set(provider.getClient(config((long) empresaAId, "AC_EMPRESA_A", "token-empresa-a")));
            });
            pool.submit(() -> {
                await(largada);
                resultadoB.set(provider.getClient(config((long) empresaBId, "AC_EMPRESA_B", "token-empresa-b")));
            });

            largada.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(resultadoA.get().getAccountSid()).isEqualTo("AC_EMPRESA_A");
        assertThat(resultadoB.get().getAccountSid()).isEqualTo("AC_EMPRESA_B");
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

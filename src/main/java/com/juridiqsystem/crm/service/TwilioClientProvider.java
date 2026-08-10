package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.config.CacheConfig;
import com.juridiqsystem.crm.model.EmpresaTwilioConfig;
import com.twilio.http.NetworkHttpClient;
import com.twilio.http.TwilioRestClient;
import org.apache.http.client.config.RequestConfig;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Resolve um TwilioRestClient por empresa, substituindo o antigo bean singleton (TwilioConfig) que
 * mantinha uma única conta Twilio global para toda a aplicação. Cada client é imutável e específico
 * de uma empresa (accountSid/authToken próprios) — nenhum estado mutável é compartilhado entre
 * requisições concorrentes de tenants diferentes, então múltiplas empresas podem enviar mensagens
 * ao mesmo tempo com segurança. Cacheado por empresaId (Caffeine, ver CacheConfig) só para não
 * reconstruir o client HTTP a cada envio — invalidate() é chamado sempre que a config Twilio da
 * empresa é salva/atualizada, para nunca operar com credenciais antigas até o TTL do cache expirar.
 */
@Component
public class TwilioClientProvider {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int SOCKET_TIMEOUT_MS = 15_000;

    private final Cache cache;

    public TwilioClientProvider(CacheManager cacheManager) {
        this.cache = cacheManager.getCache(CacheConfig.TWILIO_CLIENTS_CACHE);
    }

    public TwilioRestClient getClient(EmpresaTwilioConfig config) {
        return cache.get(config.getEmpresaId(), () -> buildClient(config));
    }

    public void invalidate(Long empresaId) {
        cache.evict(empresaId);
    }

    private TwilioRestClient buildClient(EmpresaTwilioConfig config) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .build();
        NetworkHttpClient httpClient = new NetworkHttpClient(requestConfig);
        return new TwilioRestClient.Builder(config.getAccountSid(), config.getAuthToken())
                .httpClient(httpClient)
                .build();
    }
}

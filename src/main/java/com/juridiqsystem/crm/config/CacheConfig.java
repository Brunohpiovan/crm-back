package com.juridiqsystem.crm.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine em memória (single-instance no Railway hoje; se o backend passar a rodar em múltiplas
 * instâncias, isso deixa de compartilhar cache entre elas). Cada cache é registrado com seu
 * próprio Caffeine (registerCustomCache) porque os TTLs são diferentes: "dashboard" precisa de um
 * TTL curto (dado agregado, muda a cada movimentação), enquanto tags/templates/equipes são dados
 * de referência que mudam raramente e podem ficar em cache por mais tempo. O construtor com os
 * nomes fixos mantém o CacheManager em modo não-dinâmico: usar um nome de cache não listado aqui
 * lança erro em vez de criar silenciosamente um cache sem TTL.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DASHBOARD_CACHE = "dashboard";
    public static final String TAGS_CACHE = "tags";
    public static final String TEMPLATES_CACHE = "templates";
    public static final String EQUIPES_CACHE = "equipes";
    public static final String ESCAVADOR_ORIGENS_CACHE = "escavadorOrigens";

    @Bean
    public CacheManager cacheManager() {
        // Diferente do antigo TwilioClientProvider (que cacheava um TwilioRestClient por empresa),
        // o MetaWhatsAppClient é stateless e compartilhado entre todos os tenants — o accessToken
        // vai como parâmetro em cada chamada, então não há client por empresa para cachear.
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                DASHBOARD_CACHE, TAGS_CACHE, TEMPLATES_CACHE, EQUIPES_CACHE, ESCAVADOR_ORIGENS_CACHE);

        cacheManager.registerCustomCache(DASHBOARD_CACHE,
                Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).maximumSize(1000).build());
        cacheManager.registerCustomCache(TAGS_CACHE, dadosDeReferencia());
        cacheManager.registerCustomCache(TEMPLATES_CACHE, dadosDeReferencia());
        cacheManager.registerCustomCache(EQUIPES_CACHE, dadosDeReferencia());
        // TTL bem mais longo que os demais: a lista de diários da Escavador (GET /api/v1/origens)
        // praticamente não muda e é usada a cada ativação de OAB monitorada — ver
        // EscavadorMonitoramentoDiarioApi.listarOrigens.
        cacheManager.registerCustomCache(ESCAVADOR_ORIGENS_CACHE,
                Caffeine.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).maximumSize(1).build());

        return cacheManager;
    }

    /**
     * Spec compartilhada pelos caches de listas de referência (tags, templates, equipes): baixo
     * volume, mudam raramente, TTL de 5 minutos é suficiente para reduzir consultas repetidas sem
     * risco relevante de dado desatualizado.
     */
    private com.github.benmanes.caffeine.cache.Cache<Object, Object> dadosDeReferencia() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }
}

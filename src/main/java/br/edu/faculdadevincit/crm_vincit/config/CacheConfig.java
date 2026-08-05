package br.edu.faculdadevincit.crm_vincit.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Não havia infraestrutura de cache no projeto antes do módulo Dashboard. Caffeine em memória é
 * suficiente para o TTL curto (30s) exigido pelo endpoint de agregação; se o backend passar a
 * rodar em múltiplas instâncias, isso deixa de compartilhar cache entre elas (ver observações
 * de otimização futura no README/PR do Dashboard).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DASHBOARD_CACHE = "dashboard";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(DASHBOARD_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(1000));
        return cacheManager;
    }
}

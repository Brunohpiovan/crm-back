package com.juridiqsystem.crm.service.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limiter genérico por chave (namespace:identificador), em memória, reaproveitável por qualquer
 * endpoint sensível a abuso (envio de e-mail/WhatsApp, etc.) — mesma técnica de
 * LoginRateLimiter/PasswordRecoveryRateLimiter, só parametrizável em vez de duplicar a lógica de
 * janela deslizante a cada novo caso de uso. Por instância, não global entre réplicas.
 */
@Component
public class SlidingWindowRateLimiter {

    private record Window(Instant start, AtomicInteger count) {
    }

    private static final Duration CLEANUP_MAX_AGE = Duration.ofHours(1);

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        Window current = windows.compute(key, (k, existing) -> {
            if (existing == null || Duration.between(existing.start(), now).compareTo(window) >= 0) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return current.count().get() <= maxRequests;
    }

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    void cleanupExpiredWindows() {
        Instant now = Instant.now();
        windows.entrySet().removeIf(entry -> Duration.between(entry.getValue().start(), now).compareTo(CLEANUP_MAX_AGE) >= 0);
    }
}

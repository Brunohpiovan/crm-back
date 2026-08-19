package com.juridiqsystem.crm.service.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limiter por IP, em memória, para POST /recover-password: contém spam de e-mail e reduz o
 * ritmo de scraping de e-mails cadastrados via enumeração. É por instância - se o backend rodar
 * em múltiplas réplicas (Railway), o limite é por réplica, não global; suficiente como primeira
 * camada de defesa. Um limite realmente global exigiria um contador compartilhado (ex.: Redis).
 */
@Component
public class PasswordRecoveryRateLimiter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Window(Instant start, AtomicInteger count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryAcquire(String clientIp) {
        Instant now = Instant.now();
        Window window = windows.compute(clientIp, (ip, existing) -> {
            if (existing == null || Duration.between(existing.start(), now).compareTo(WINDOW) >= 0) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });
        return window.count().get() <= MAX_REQUESTS;
    }

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    void cleanupExpiredWindows() {
        Instant now = Instant.now();
        windows.entrySet().removeIf(entry -> Duration.between(entry.getValue().start(), now).compareTo(WINDOW) >= 0);
    }
}

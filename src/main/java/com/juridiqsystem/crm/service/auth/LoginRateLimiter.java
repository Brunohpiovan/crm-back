package com.juridiqsystem.crm.service.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limiter por IP+empresa+login, em memória, para POST /auth/login: contém brute-force de senha
 * contra uma conta específica sem punir outros usuários logando com sucesso do mesmo IP (ex.:
 * escritório atrás de um NAT). Só tentativas malsucedidas contam pro limite — login correto reseta
 * a janela. Mesma ressalva de PasswordRecoveryRateLimiter: é por instância, não global entre réplicas.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private record Window(Instant start, AtomicInteger count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Window window = windows.get(key);
        if (window == null) {
            return false;
        }
        if (Duration.between(window.start(), Instant.now()).compareTo(WINDOW) >= 0) {
            return false;
        }
        return window.count().get() >= MAX_ATTEMPTS;
    }

    public void registerFailure(String key) {
        Instant now = Instant.now();
        windows.compute(key, (k, existing) -> {
            if (existing == null || Duration.between(existing.start(), now).compareTo(WINDOW) >= 0) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    public void reset(String key) {
        windows.remove(key);
    }

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    void cleanupExpiredWindows() {
        Instant now = Instant.now();
        windows.entrySet().removeIf(entry -> Duration.between(entry.getValue().start(), now).compareTo(WINDOW) >= 0);
    }
}

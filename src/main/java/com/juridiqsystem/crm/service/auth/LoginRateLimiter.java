package com.juridiqsystem.crm.service.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limiter por IP+empresa (não por conta), em memória, para POST /auth/login: 5 tentativas
 * malsucedidas contra QUALQUER conta daquela empresa, vindas do mesmo IP, bloqueiam novas
 * tentativas de login para aquela empresa a partir daquele IP — inclusive com credenciais
 * válidas de outra conta — até a janela expirar. De propósito não isola por login: se a chave
 * incluísse a conta tentada, bastaria trocar de e-mail a cada 5 erros pra nunca ser bloqueado.
 * Um login bem-sucedido reseta a janela inteira do IP+empresa. Mesma ressalva de
 * PasswordRecoveryRateLimiter: é por instância, não global entre réplicas.
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

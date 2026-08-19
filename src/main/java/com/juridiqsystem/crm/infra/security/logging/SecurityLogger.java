package com.juridiqsystem.crm.infra.security.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Ponto único pra registrar eventos de segurança: sempre grava no log local da aplicação (nível
 * INFO/WARN, útil pra auditoria mesmo sem Discord) e publica de forma assíncrona no Discord via
 * {@link DiscordWebhookService}, que monta um embed formatado a partir dos mesmos campos. Nunca
 * passe aqui senha, hash de senha, token JWT completo, Authorization header ou qualquer segredo —
 * o texto é redigido defensivamente, mas a régua real é: não construa a mensagem com esses dados
 * em primeiro lugar.
 */
@Slf4j
@Component
public class SecurityLogger {

    // Reduz o risco de um JWT completo vazar pro Discord caso alguém passe, por engano, uma
    // mensagem de exceção que contenha um (ex.: "Bearer eyJhbGciOi...").
    private static final Pattern JWT_LIKE = Pattern.compile("eyJ[\\w-]+\\.[\\w-]+\\.[\\w-]+");

    @Autowired
    private DiscordWebhookService discordWebhookService;

    public void log(SecurityEventType type, String details, String actor, String ip, String path) {
        String redactedDetails = redact(details);

        log.info("[SECURITY] {} actor={} ip={} path={} details={}", type, actor, ip, path, redactedDetails);
        discordWebhookService.send(type, actor, ip, path, redactedDetails);
    }

    private String redact(String text) {
        if (text == null) {
            return null;
        }
        return JWT_LIKE.matcher(text).replaceAll("[REDACTED_TOKEN]");
    }
}

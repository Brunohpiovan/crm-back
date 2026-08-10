package com.juridiqsystem.crm.infra.security.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

/**
 * Ponto único pra registrar eventos de segurança: sempre grava no log local da aplicação (nível
 * INFO/WARN, útil pra auditoria mesmo sem Discord) e publica de forma assíncrona no Discord via
 * {@link DiscordWebhookService}. Nunca passe aqui senha, hash de senha, token JWT completo,
 * Authorization header ou qualquer segredo — o texto é redigido defensivamente, mas a régua real
 * é: não construa a mensagem com esses dados em primeiro lugar.
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
        discordWebhookService.send(type, buildDiscordMessage(type, redactedDetails, actor, ip, path));
    }

    private String buildDiscordMessage(SecurityEventType type, String details, String actor, String ip, String path) {
        StringBuilder message = new StringBuilder("**").append(type).append("** — ").append(OffsetDateTime.now());
        if (actor != null && !actor.isBlank()) {
            message.append("\nUsuário: `").append(actor).append('`');
        }
        if (ip != null && !ip.isBlank()) {
            message.append("\nIP: `").append(ip).append('`');
        }
        if (path != null && !path.isBlank()) {
            message.append("\nRota: `").append(path).append('`');
        }
        if (details != null && !details.isBlank()) {
            message.append("\nDetalhes: ").append(details);
        }
        return message.toString();
    }

    private String redact(String text) {
        if (text == null) {
            return null;
        }
        return JWT_LIKE.matcher(text).replaceAll("[REDACTED_TOKEN]");
    }
}

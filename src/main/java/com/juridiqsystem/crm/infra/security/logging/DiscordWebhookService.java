package com.juridiqsystem.crm.infra.security.logging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.juridiqsystem.crm.infra.security.logging.SecurityEventType.*;

/**
 * Publica eventos de segurança num canal do Discord via webhook, como embed formatado (título com
 * ícone, cor por severidade, campos estruturados de usuário/IP/rota e a descrição com o detalhe do
 * evento) em vez de texto corrido. Nunca deve afetar o request que originou o evento: método
 * assíncrono (roda fora da thread da requisição), timeout curto, poucas tentativas com backoff, e
 * qualquer falha (webhook fora do ar, indisponível, não configurado) só gera um log local — nunca
 * propaga exceção pro chamador.
 */
@Slf4j
@Service
public class DiscordWebhookService {

    private static final int MAX_ATTEMPTS = 2;
    private static final Duration RETRY_DELAY = Duration.ofMillis(500);
    private static final int FIELD_VALUE_LIMIT = 1000;
    private static final int DESCRIPTION_LIMIT = 3800;

    private static final String FOOTER_TEXT = "JuridicSystem CRM · Segurança";

    // Cores no formato decimal esperado pelo campo "color" do embed (equivalente a 0xRRGGBB).
    private static final int COLOR_SUCCESS = 0x2ECC71; // verde — ação concluída normalmente
    private static final int COLOR_WARNING = 0xF5A623; // âmbar — merece atenção, ainda não é um ataque confirmado
    private static final int COLOR_DANGER = 0xE74C3C; // vermelho — tentativa de acesso indevido/bloqueio
    private static final int COLOR_INFO = 0x5865F2; // azul — ação administrativa legítima, só pra auditoria

    private static final Map<SecurityEventType, String> TITLES = new EnumMap<>(SecurityEventType.class);
    private static final Map<SecurityEventType, Integer> COLORS = new EnumMap<>(SecurityEventType.class);

    static {
        TITLES.put(LOGIN_SUCCESS, "🔓 Login realizado");
        TITLES.put(LOGIN_FAILED, "❌ Falha no login");
        TITLES.put(LOGOUT, "🚪 Logout");
        TITLES.put(PASSWORD_CHANGED, "🔑 Senha alterada");
        TITLES.put(PASSWORD_RESET_REQUEST, "✉️ Pedido de redefinição de senha");
        TITLES.put(PASSWORD_RESET_SUCCESS, "✅ Senha redefinida");
        TITLES.put(ACCOUNT_LOCKED, "🔒 Login recusado — conta bloqueada");
        TITLES.put(ACCESS_DENIED, "🚫 Acesso negado");
        TITLES.put(UNAUTHORIZED_ACCESS, "🛑 Requisição não autenticada");
        TITLES.put(INVALID_TOKEN, "⚠️ Token inválido");
        TITLES.put(EXPIRED_TOKEN, "⌛ Sessão expirada ou revogada");
        TITLES.put(RATE_LIMIT_TRIGGERED, "🚦 Limite de tentativas atingido");
        TITLES.put(SUSPICIOUS_REQUEST, "🕵️ Requisição suspeita");
        TITLES.put(ADMIN_ACTION, "🛠️ Ação administrativa");
        TITLES.put(RESOURCE_ACCESS_DENIED, "🔐 Acesso a recurso negado");
        TITLES.put(ADMIN_ROUTE_DENIED, "🛡️ Tentativa de acesso administrativo bloqueada");

        COLORS.put(LOGIN_SUCCESS, COLOR_SUCCESS);
        COLORS.put(LOGOUT, COLOR_SUCCESS);
        COLORS.put(PASSWORD_CHANGED, COLOR_SUCCESS);
        COLORS.put(PASSWORD_RESET_SUCCESS, COLOR_SUCCESS);
        COLORS.put(PASSWORD_RESET_REQUEST, COLOR_WARNING);
        COLORS.put(RATE_LIMIT_TRIGGERED, COLOR_WARNING);
        COLORS.put(ACCOUNT_LOCKED, COLOR_WARNING);
        COLORS.put(LOGIN_FAILED, COLOR_DANGER);
        COLORS.put(ACCESS_DENIED, COLOR_DANGER);
        COLORS.put(UNAUTHORIZED_ACCESS, COLOR_DANGER);
        COLORS.put(INVALID_TOKEN, COLOR_DANGER);
        COLORS.put(EXPIRED_TOKEN, COLOR_DANGER);
        COLORS.put(SUSPICIOUS_REQUEST, COLOR_DANGER);
        COLORS.put(RESOURCE_ACCESS_DENIED, COLOR_DANGER);
        COLORS.put(ADMIN_ROUTE_DENIED, COLOR_DANGER);
        COLORS.put(ADMIN_ACTION, COLOR_INFO);
    }

    @Autowired
    private DiscordWebhookProperties properties;

    // Chaves de webhooksByEvent normalizadas (maiúsculas) uma vez, pra não depender de como o
    // binder do Spring capitaliza a chave do Map vinda do properties/env var.
    private Map<String, String> webhooksByEventNormalized = Map.of();

    @PostConstruct
    void normalizeEventWebhooks() {
        Map<String, String> normalized = new HashMap<>();
        properties.getWebhooksByEvent().forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                normalized.put(key.toUpperCase(), value);
            }
        });
        webhooksByEventNormalized = normalized;
    }

    private final RestClient restClient = RestClient.builder()
            .requestFactory(buildRequestFactory())
            .build();

    private static SimpleClientHttpRequestFactory buildRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return factory;
    }

    @Async
    public void send(SecurityEventType type, String actor, String ip, String path, String details) {
        String webhookUrl = webhooksByEventNormalized.getOrDefault(type.name(), properties.getWebhookUrl());
        if (webhookUrl == null || webhookUrl.isBlank()) {
            // Sem webhook configurado pra esse evento nem um padrão (ex.: ambiente local/dev) —
            // ignora silenciosamente, não é erro.
            return;
        }

        Map<String, Object> payload = buildPayload(type, actor, ip, path, details);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restClient.post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                return;
            } catch (RestClientException e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.warn("Falha ao enviar evento de segurança pro Discord após {} tentativa(s): {}", MAX_ATTEMPTS, e.getMessage());
                    return;
                }
                sleep(RETRY_DELAY);
            }
        }
    }

    private Map<String, Object> buildPayload(SecurityEventType type, String actor, String ip, String path, String details) {
        Map<String, Object> embed = new HashMap<>();
        embed.put("title", TITLES.getOrDefault(type, type.name()));
        embed.put("color", COLORS.getOrDefault(type, COLOR_INFO));
        embed.put("timestamp", OffsetDateTime.now().toString());
        embed.put("footer", Map.of("text", FOOTER_TEXT));

        if (details != null && !details.isBlank()) {
            embed.put("description", truncate(details, DESCRIPTION_LIMIT));
        }

        List<Map<String, Object>> fields = new ArrayList<>();
        addInlineField(fields, "Usuário", actor);
        addInlineField(fields, "IP", ip);
        addInlineField(fields, "Rota", path);
        if (!fields.isEmpty()) {
            embed.put("fields", fields);
        }

        return Map.of("embeds", List.of(embed));
    }

    private void addInlineField(List<Map<String, Object>> fields, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Map<String, Object> field = new HashMap<>();
        field.put("name", name);
        field.put("value", "`" + truncate(value, FIELD_VALUE_LIMIT) + "`");
        field.put("inline", true);
        fields.add(field);
    }

    private String truncate(String text, int limit) {
        return text.length() > limit ? text.substring(0, limit - 1) + "…" : text;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

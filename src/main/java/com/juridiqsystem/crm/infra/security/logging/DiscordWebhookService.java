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
import java.util.HashMap;
import java.util.Map;

/**
 * Publica eventos de segurança num canal do Discord via webhook. Nunca deve afetar o request que
 * originou o evento: método assíncrono (roda fora da thread da requisição), timeout curto, poucas
 * tentativas com backoff, e qualquer falha (webhook fora do ar, indisponível, não configurado) só
 * gera um log local — nunca propaga exceção pro chamador.
 */
@Slf4j
@Service
public class DiscordWebhookService {

    private static final int MAX_ATTEMPTS = 2;
    private static final Duration RETRY_DELAY = Duration.ofMillis(500);
    private static final int DISCORD_MESSAGE_LIMIT = 2000;

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
    public void send(SecurityEventType type, String content) {
        String webhookUrl = webhooksByEventNormalized.getOrDefault(type.name(), properties.getWebhookUrl());
        if (webhookUrl == null || webhookUrl.isBlank()) {
            // Sem webhook configurado pra esse evento nem um padrão (ex.: ambiente local/dev) —
            // ignora silenciosamente, não é erro.
            return;
        }

        String truncatedContent = content.length() > DISCORD_MESSAGE_LIMIT
                ? content.substring(0, DISCORD_MESSAGE_LIMIT - 3) + "..."
                : content;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restClient.post()
                        .uri(webhookUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("content", truncatedContent))
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

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

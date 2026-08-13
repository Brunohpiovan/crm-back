package com.juridiqsystem.crm.infra.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juridiqsystem.crm.infra.whatsapp.dto.MetaMediaUrlResult;
import com.juridiqsystem.crm.infra.whatsapp.dto.MetaPhoneNumberDetails;
import com.juridiqsystem.crm.infra.whatsapp.dto.MetaSendMessageResult;
import com.juridiqsystem.crm.infra.whatsapp.dto.MetaTokenExchangeResult;
import com.juridiqsystem.crm.service.exceptions.MetaAuthenticationException;
import com.juridiqsystem.crm.service.exceptions.MetaIntegrationException;
import com.juridiqsystem.crm.service.exceptions.MetaMessageException;
import com.juridiqsystem.crm.service.exceptions.MetaRateLimitException;
import com.juridiqsystem.crm.service.exceptions.MetaTemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Camada centralizada de comunicação com a Graph API (Meta WhatsApp Cloud API). Nenhuma outra
 * classe do projeto deve montar uma URL "/v.../{phone-number-id}/messages" ou usar RestClient/
 * WebClient diretamente para falar com a Meta — tudo passa por aqui. Arquitetura:
 * WhatsAppService -&gt; MetaWhatsAppClient -&gt; Graph API (ver docs/whatsapp/ARCHITECTURE.md).
 */
@Slf4j
@Component
public class MetaWhatsAppClient {

    private final RestClient restClient;
    private final MetaWhatsAppProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MetaWhatsAppClient(MetaWhatsAppProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getGraphBaseUrl() + "/" + properties.getGraphApiVersion())
                .requestFactory(requestFactory)
                .build();
    }

    @Retryable(retryFor = ResourceAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public MetaSendMessageResult sendText(String phoneNumberId, String accessToken, String to, String body) {
        Map<String, Object> payload = baseMessagePayload(to, "text");
        payload.put("text", Map.of("body", body != null ? body : ""));
        return send(phoneNumberId, accessToken, payload, false);
    }

    @Retryable(retryFor = ResourceAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public MetaSendMessageResult sendMedia(String phoneNumberId, String accessToken, String to, String mediaType, String link, String caption) {
        Map<String, Object> payload = baseMessagePayload(to, mediaType);
        Map<String, Object> mediaObject = new LinkedHashMap<>();
        mediaObject.put("link", link);
        if (caption != null && !caption.isBlank() && ("image".equals(mediaType) || "video".equals(mediaType) || "document".equals(mediaType))) {
            mediaObject.put("caption", caption);
        }
        payload.put(mediaType, mediaObject);
        return send(phoneNumberId, accessToken, payload, false);
    }

    @Retryable(retryFor = ResourceAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public MetaSendMessageResult sendTemplate(String phoneNumberId, String accessToken, String to, String templateName, String languageCode, List<String> bodyParams) {
        Map<String, Object> payload = baseMessagePayload(to, "template");
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", languageCode));
        if (bodyParams != null && !bodyParams.isEmpty()) {
            List<Map<String, String>> parameters = bodyParams.stream()
                    .map(param -> Map.of("type", "text", "text", param))
                    .toList();
            template.put("components", List.of(Map.of("type", "body", "parameters", parameters)));
        }
        payload.put("template", template);
        return send(phoneNumberId, accessToken, payload, true);
    }

    private Map<String, Object> baseMessagePayload(String to, String type) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", to);
        payload.put("type", type);
        return payload;
    }

    private MetaSendMessageResult send(String phoneNumberId, String accessToken, Map<String, Object> payload, boolean isTemplate) {
        try {
            JsonNode response = restClient.post()
                    .uri("/{phoneNumberId}/messages", phoneNumberId)
                    .headers(headers -> authHeaders(headers, accessToken))
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            String messageId = response.path("messages").path(0).path("id").asText(null);
            return new MetaSendMessageResult(messageId);
        } catch (RestClientResponseException e) {
            throw mapError(e, isTemplate);
        } catch (ResourceAccessException e) {
            throw e; // deixa o @Retryable interceptar; se esgotar tentativas, sobe como está
        }
    }

    @Retryable(retryFor = ResourceAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public MetaMediaUrlResult fetchMediaUrl(String mediaId, String accessToken) {
        try {
            JsonNode response = restClient.get()
                    .uri("/{mediaId}", mediaId)
                    .headers(headers -> authHeaders(headers, accessToken))
                    .retrieve()
                    .body(JsonNode.class);
            return new MetaMediaUrlResult(response.path("url").asText(null), response.path("mime_type").asText(null));
        } catch (RestClientResponseException e) {
            throw mapError(e, false);
        }
    }

    /**
     * A URL retornada por fetchMediaUrl é temporária e só pode ser baixada com o mesmo access
     * token da integração (Authorization: Bearer) — diferente da Twilio, que usava Basic Auth
     * accountSid:authToken.
     */
    @Retryable(retryFor = ResourceAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public byte[] downloadMedia(String url, String accessToken) {
        try {
            return restClient.get()
                    .uri(url)
                    .headers(headers -> authHeaders(headers, accessToken))
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientResponseException e) {
            throw mapError(e, false);
        }
    }

    /**
     * Troca o código de autorização recebido pelo Embedded Signup (FB.login callback) por um
     * token de acesso de negócio, via o endpoint padrão de OAuth da Meta (Debug/Graph
     * "oauth/access_token" com grant_type=authorization_code, usando App Id/Secret do SaaS).
     */
    public MetaTokenExchangeResult exchangeCodeForToken(String code) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/oauth/access_token")
                            .queryParam("client_id", properties.getAppId())
                            .queryParam("client_secret", properties.getAppSecret())
                            .queryParam("code", code)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            String accessToken = response.path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                throw new MetaAuthenticationException("A Meta não retornou um access token válido para o código de autorização informado.");
            }
            Long expiresIn = response.hasNonNull("expires_in") ? response.path("expires_in").asLong() : null;
            return new MetaTokenExchangeResult(accessToken, expiresIn);
        } catch (RestClientResponseException e) {
            throw mapError(e, false);
        }
    }

    public MetaPhoneNumberDetails fetchPhoneNumberDetails(String phoneNumberId, String accessToken) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/{phoneNumberId}")
                            .queryParam("fields", "display_phone_number,verified_name")
                            .build(phoneNumberId))
                    .headers(headers -> authHeaders(headers, accessToken))
                    .retrieve()
                    .body(JsonNode.class);
            return new MetaPhoneNumberDetails(
                    response.path("display_phone_number").asText(null),
                    response.path("verified_name").asText(null));
        } catch (RestClientResponseException e) {
            throw mapError(e, false);
        }
    }

    /**
     * Inscreve o Meta App do SaaS para receber webhooks da WABA do cliente — sem isso, mensagens
     * enviadas para o número da empresa nunca chegam no nosso /whatsapp/webhook. Necessário uma
     * vez por WABA, ao final do Embedded Signup.
     */
    public void subscribeAppToWaba(String wabaId, String accessToken) {
        try {
            restClient.post()
                    .uri("/{wabaId}/subscribed_apps", wabaId)
                    .headers(headers -> authHeaders(headers, accessToken))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw mapError(e, false);
        }
    }

    private void authHeaders(HttpHeaders headers, String accessToken) {
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    }

    /**
     * Traduz a resposta de erro crua da Graph API ({"error":{"code","type","message",...}}) numa
     * das exceções de domínio Meta*Exception — o payload cru nunca chega ao controller/frontend.
     */
    private RuntimeException mapError(RestClientResponseException e, boolean isTemplateCall) {
        String metaMessage = extractMetaErrorMessage(e);
        HttpStatusCode status = e.getStatusCode();
        log.warn("Erro retornado pela Graph API: status={} corpo={}", status.value(), safeBody(e));

        if (status.value() == 401 || status.value() == 403) {
            return new MetaAuthenticationException("Autenticação com a Meta falhou. Reconecte o WhatsApp desta empresa.");
        }
        if (status.value() == 429) {
            return new MetaRateLimitException("Limite de envio da Meta excedido. Tente novamente em instantes.");
        }
        if (isTemplateCall) {
            return new MetaTemplateException(metaMessage != null ? metaMessage : "Falha ao enviar template do WhatsApp.");
        }
        if (status.is5xxServerError()) {
            return new MetaIntegrationException("A API do WhatsApp está indisponível no momento.");
        }
        return new MetaMessageException(metaMessage != null ? metaMessage : "Falha ao enviar mensagem via WhatsApp.");
    }

    private String extractMetaErrorMessage(RestClientResponseException e) {
        try {
            JsonNode error = objectMapper.readTree(e.getResponseBodyAsByteArray()).path("error");
            String message = error.path("message").asText(null);
            return message;
        } catch (Exception parseError) {
            return null;
        }
    }

    private String safeBody(RestClientResponseException e) {
        // Nunca loga Authorization/App Secret — só o corpo de erro da Meta, que já não contém credenciais.
        return e.getResponseBodyAsString();
    }
}

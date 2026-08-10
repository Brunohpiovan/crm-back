package com.juridiqsystem.crm.infra.security.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Config de roteamento dos eventos de segurança pro Discord. `webhookUrl` é o fallback usado
 * quando um SecurityEventType não tem entrada em `webhooksByEvent` — permite criar canais
 * separados por evento (ou por grupo de eventos, apontando vários nomes pra mesma URL) sem
 * precisar mexer em código, só em variável de ambiente/application.properties.
 */
@Component
@ConfigurationProperties(prefix = "security.discord")
public class DiscordWebhookProperties {

    private String webhookUrl = "";
    private Map<String, String> webhooksByEvent = new HashMap<>();

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public Map<String, String> getWebhooksByEvent() {
        return webhooksByEvent;
    }

    public void setWebhooksByEvent(Map<String, String> webhooksByEvent) {
        this.webhooksByEvent = webhooksByEvent;
    }
}

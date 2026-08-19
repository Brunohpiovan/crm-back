package com.juridiqsystem.crm.infra.escavador;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração centralizada da integração com a Escavador Business API — nenhuma outra classe do
 * projeto deve ler "escavador.api.*"/"ESCAVADOR_*" diretamente via @Value. Conta única e
 * compartilhada do juriq-crm (não é a Empresa-cliente que traz o próprio token — ver
 * docs/integrations/escavador-api.md e pesquisa de mercado em memória do projeto).
 */
@Component
@ConfigurationProperties(prefix = "escavador.api")
public class EscavadorApiProperties {

    /** Personal Access Token (PAT) — nunca logado nem exposto ao frontend. */
    private String token;
    private int connectTimeoutMs = 10_000;
    private int readTimeoutMs = 15_000;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}

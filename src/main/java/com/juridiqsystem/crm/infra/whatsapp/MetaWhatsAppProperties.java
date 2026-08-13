package com.juridiqsystem.crm.infra.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração centralizada da integração com a Meta WhatsApp Cloud API — nenhuma outra classe do
 * projeto deve ler "meta.whatsapp.*"/"META_*" diretamente via @Value. Ver ETAPA 38 (variáveis de
 * ambiente) e docs/whatsapp/META_SETUP.md para onde obter cada valor no painel da Meta.
 */
@Component
@ConfigurationProperties(prefix = "meta.whatsapp")
public class MetaWhatsAppProperties {

    /** Ex.: "v23.0" — nunca hardcode a versão da Graph API em URLs espalhadas pelo código. */
    private String graphApiVersion;
    private String graphBaseUrl;
    /** App ID do Meta Developer App do SaaS — público, pode ser exposto ao frontend (Facebook JS SDK). */
    private String appId;
    /** App Secret — nunca exposto ao frontend; usado só para validar assinatura de webhook e trocar código do Embedded Signup. */
    private String appSecret;
    /** Configuration ID do fluxo de Embedded Signup, criado no painel da Meta. */
    private String configId;
    /** Token arbitrário que nós escolhemos e cadastramos no painel da Meta, usado na verificação GET /whatsapp/webhook. */
    private String verifyToken;
    private int connectTimeoutMs = 10_000;
    private int readTimeoutMs = 15_000;

    public String getGraphApiVersion() {
        return graphApiVersion;
    }

    public void setGraphApiVersion(String graphApiVersion) {
        this.graphApiVersion = graphApiVersion;
    }

    public String getGraphBaseUrl() {
        return graphBaseUrl;
    }

    public void setGraphBaseUrl(String graphBaseUrl) {
        this.graphBaseUrl = graphBaseUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
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

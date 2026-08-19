package com.juridiqsystem.crm.model.enums;

/**
 * Estado da integração WhatsApp (Meta Cloud API) de uma empresa. Usado tanto no backend quanto
 * refletido 1:1 na tela de Configurações &gt; WhatsApp do frontend.
 */
public enum WhatsAppIntegrationStatus {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
    DISCONNECTED
}

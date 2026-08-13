package com.juridiqsystem.crm.model.enums;

/**
 * Status de entrega de uma mensagem enviada via WhatsApp (Meta Cloud API). O restante do sistema
 * nunca deve depender dos nomes crus enviados pelo webhook de status da Meta ("sent", "delivered",
 * "read", "failed") — MetaWebhookMapper é o único lugar que faz esse de-para.
 */
public enum MessageStatus {
    PENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

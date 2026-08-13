package com.juridiqsystem.crm.model.whatsapp;

import java.time.Instant;

/**
 * Modelo interno de uma mensagem recebida via WhatsApp, já traduzido do JSON bruto da Meta por
 * MetaWebhookMapper. A partir daqui (WhatsAppService em diante), nenhuma regra de negócio manipula
 * o payload cru da Graph API — só este record.
 */
public record IncomingWhatsAppMessage(
        String externalMessageId,
        String phoneNumberId,
        String from,
        MessageType type,
        String text,
        String mediaId,
        String mediaMimeType,
        String profileName,
        Instant timestamp
) {
}

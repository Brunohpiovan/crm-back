package com.juridiqsystem.crm.model.whatsapp;

import java.util.List;

/**
 * Resultado do parsing de um evento de webhook da Meta (POST /whatsapp/webhook), já separado em
 * mensagens recebidas e eventos de status. phoneNumberId vem de value.metadata.phone_number_id e é
 * a chave usada para resolver o tenant (ver WhatsAppIntegrationRepository.findByPhoneNumberIdIgnoringTenant).
 * Pode ser null se o evento não tiver relação com mensagens (ex.: outros campos de webhook da conta).
 */
public record WhatsAppWebhookPayload(
        String phoneNumberId,
        List<IncomingWhatsAppMessage> messages,
        List<WhatsAppStatusEvent> statuses
) {
}

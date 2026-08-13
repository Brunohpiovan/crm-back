package com.juridiqsystem.crm.infra.whatsapp.dto;

/** messageId = wamid retornado por messages[0].id na resposta de POST /{phone-number-id}/messages. */
public record MetaSendMessageResult(String messageId) {
}

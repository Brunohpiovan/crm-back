package com.juridiqsystem.crm.model.whatsapp;

import com.juridiqsystem.crm.model.enums.MessageStatus;

import java.time.Instant;

/** Evento de status de entrega (sent/delivered/read/failed), já traduzido do JSON bruto da Meta. */
public record WhatsAppStatusEvent(
        String externalMessageId,
        String phoneNumberId,
        MessageStatus status,
        Instant timestamp,
        Integer errorCode,
        String errorMessage
) {
}

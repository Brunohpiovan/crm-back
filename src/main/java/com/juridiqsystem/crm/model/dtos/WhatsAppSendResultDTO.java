package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.enums.MessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do envio de uma mensagem via WhatsApp (Meta Cloud API). DTO interno — nunca o payload cru da Graph API.")
public record WhatsAppSendResultDTO(String externalMessageId, MessageStatus status) {
}

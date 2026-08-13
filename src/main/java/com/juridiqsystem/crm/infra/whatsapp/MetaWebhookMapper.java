package com.juridiqsystem.crm.infra.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juridiqsystem.crm.model.enums.MessageStatus;
import com.juridiqsystem.crm.model.whatsapp.IncomingWhatsAppMessage;
import com.juridiqsystem.crm.model.whatsapp.MessageType;
import com.juridiqsystem.crm.model.whatsapp.WhatsAppStatusEvent;
import com.juridiqsystem.crm.model.whatsapp.WhatsAppWebhookPayload;
import com.juridiqsystem.crm.service.exceptions.MetaWebhookException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Único lugar do sistema que entende o formato bruto do webhook da Meta
 * ({"object":"whatsapp_business_account","entry":[{"changes":[{"value":{...}}]}]}). Converte para
 * o modelo interno (IncomingWhatsAppMessage/WhatsAppStatusEvent) — a regra de negócio (WhatsAppService)
 * nunca acessa o JsonNode diretamente. Ver docs/whatsapp/WEBHOOKS.md para exemplos de payload.
 */
@Component
public class MetaWebhookMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public WhatsAppWebhookPayload parse(String rawBody) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw new MetaWebhookException("Corpo do webhook Meta não é um JSON válido.");
        }

        List<IncomingWhatsAppMessage> messages = new ArrayList<>();
        List<WhatsAppStatusEvent> statuses = new ArrayList<>();
        String phoneNumberId = null;

        for (JsonNode entry : root.path("entry")) {
            for (JsonNode change : entry.path("changes")) {
                JsonNode value = change.path("value");
                if (!value.path("messaging_product").asText("").equals("whatsapp")) {
                    continue;
                }
                String currentPhoneNumberId = value.path("metadata").path("phone_number_id").asText(null);
                if (currentPhoneNumberId != null) {
                    phoneNumberId = currentPhoneNumberId;
                }

                String profileName = extractProfileName(value);
                for (JsonNode messageNode : value.path("messages")) {
                    messages.add(parseMessage(messageNode, currentPhoneNumberId, profileName));
                }
                for (JsonNode statusNode : value.path("statuses")) {
                    statuses.add(parseStatus(statusNode, currentPhoneNumberId));
                }
            }
        }

        return new WhatsAppWebhookPayload(phoneNumberId, messages, statuses);
    }

    private String extractProfileName(JsonNode value) {
        JsonNode contacts = value.path("contacts");
        if (contacts.isArray() && !contacts.isEmpty()) {
            return contacts.get(0).path("profile").path("name").asText(null);
        }
        return null;
    }

    private IncomingWhatsAppMessage parseMessage(JsonNode messageNode, String phoneNumberId, String profileName) {
        String type = messageNode.path("type").asText("");
        MessageType messageType = switch (type) {
            case "text" -> MessageType.TEXT;
            case "image" -> MessageType.IMAGE;
            case "audio" -> MessageType.AUDIO;
            case "document" -> MessageType.DOCUMENT;
            case "video" -> MessageType.VIDEO;
            default -> MessageType.UNKNOWN;
        };

        String text = messageType == MessageType.TEXT ? messageNode.path("text").path("body").asText(null) : null;
        JsonNode mediaNode = switch (messageType) {
            case IMAGE -> messageNode.path("image");
            case AUDIO -> messageNode.path("audio");
            case DOCUMENT -> messageNode.path("document");
            case VIDEO -> messageNode.path("video");
            default -> null;
        };
        String mediaId = mediaNode != null ? mediaNode.path("id").asText(null) : null;
        String mediaMimeType = mediaNode != null ? mediaNode.path("mime_type").asText(null) : null;
        String caption = mediaNode != null ? mediaNode.path("caption").asText(null) : null;

        return new IncomingWhatsAppMessage(
                messageNode.path("id").asText(null),
                phoneNumberId,
                messageNode.path("from").asText(null),
                messageType,
                text != null ? text : caption,
                mediaId,
                mediaMimeType,
                profileName,
                parseTimestamp(messageNode.path("timestamp").asText(null))
        );
    }

    private WhatsAppStatusEvent parseStatus(JsonNode statusNode, String phoneNumberId) {
        String rawStatus = statusNode.path("status").asText("");
        MessageStatus status = switch (rawStatus) {
            case "sent" -> MessageStatus.SENT;
            case "delivered" -> MessageStatus.DELIVERED;
            case "read" -> MessageStatus.READ;
            case "failed" -> MessageStatus.FAILED;
            default -> null;
        };

        JsonNode errors = statusNode.path("errors");
        Integer errorCode = null;
        String errorMessage = null;
        if (errors.isArray() && !errors.isEmpty()) {
            errorCode = errors.get(0).path("code").asInt();
            errorMessage = errors.get(0).path("title").asText(null);
        }

        return new WhatsAppStatusEvent(
                statusNode.path("id").asText(null),
                phoneNumberId,
                status,
                parseTimestamp(statusNode.path("timestamp").asText(null)),
                errorCode,
                errorMessage
        );
    }

    private Instant parseTimestamp(String epochSeconds) {
        if (epochSeconds == null || epochSeconds.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(epochSeconds));
        } catch (NumberFormatException e) {
            return Instant.now();
        }
    }
}

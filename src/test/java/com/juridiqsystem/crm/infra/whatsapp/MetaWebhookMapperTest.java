package com.juridiqsystem.crm.infra.whatsapp;

import com.juridiqsystem.crm.model.enums.MessageStatus;
import com.juridiqsystem.crm.model.whatsapp.MessageType;
import com.juridiqsystem.crm.model.whatsapp.WhatsAppWebhookPayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetaWebhookMapperTest {

    private final MetaWebhookMapper mapper = new MetaWebhookMapper();

    @Test
    void extraiMensagemDeTextoEPhoneNumberId() {
        String body = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "id": "waba-1",
                    "changes": [{
                      "field": "messages",
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": {"display_phone_number": "551140049999", "phone_number_id": "1234567890"},
                        "contacts": [{"profile": {"name": "Cliente Teste"}, "wa_id": "5511987654321"}],
                        "messages": [{
                          "from": "5511987654321",
                          "id": "wamid.ABC123",
                          "timestamp": "1700000000",
                          "type": "text",
                          "text": {"body": "Olá, preciso de ajuda"}
                        }]
                      }
                    }]
                  }]
                }
                """;

        WhatsAppWebhookPayload payload = mapper.parse(body);

        assertThat(payload.phoneNumberId()).isEqualTo("1234567890");
        assertThat(payload.messages()).hasSize(1);
        assertThat(payload.statuses()).isEmpty();

        var message = payload.messages().get(0);
        assertThat(message.externalMessageId()).isEqualTo("wamid.ABC123");
        assertThat(message.from()).isEqualTo("5511987654321");
        assertThat(message.type()).isEqualTo(MessageType.TEXT);
        assertThat(message.text()).isEqualTo("Olá, preciso de ajuda");
        assertThat(message.profileName()).isEqualTo("Cliente Teste");
    }

    @Test
    void extraiMensagemDeImagemComMediaId() {
        String body = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": {"phone_number_id": "1234567890"},
                        "messages": [{
                          "from": "5511987654321",
                          "id": "wamid.IMG1",
                          "timestamp": "1700000001",
                          "type": "image",
                          "image": {"id": "media-id-1", "mime_type": "image/jpeg", "caption": "Veja isso"}
                        }]
                      }
                    }]
                  }]
                }
                """;

        var message = mapper.parse(body).messages().get(0);

        assertThat(message.type()).isEqualTo(MessageType.IMAGE);
        assertThat(message.mediaId()).isEqualTo("media-id-1");
        assertThat(message.mediaMimeType()).isEqualTo("image/jpeg");
        assertThat(message.text()).isEqualTo("Veja isso");
    }

    @Test
    void extraiEventoDeStatus() {
        String body = """
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "changes": [{
                      "value": {
                        "messaging_product": "whatsapp",
                        "metadata": {"phone_number_id": "1234567890"},
                        "statuses": [{
                          "id": "wamid.ABC123",
                          "status": "delivered",
                          "timestamp": "1700000002",
                          "recipient_id": "5511987654321"
                        }]
                      }
                    }]
                  }]
                }
                """;

        WhatsAppWebhookPayload payload = mapper.parse(body);

        assertThat(payload.messages()).isEmpty();
        assertThat(payload.statuses()).hasSize(1);
        var status = payload.statuses().get(0);
        assertThat(status.externalMessageId()).isEqualTo("wamid.ABC123");
        assertThat(status.status()).isEqualTo(MessageStatus.DELIVERED);
    }

    @Test
    void payloadSemMensagensRetornaPhoneNumberIdNulo() {
        String body = "{\"object\":\"whatsapp_business_account\",\"entry\":[]}";

        WhatsAppWebhookPayload payload = mapper.parse(body);

        assertThat(payload.phoneNumberId()).isNull();
        assertThat(payload.messages()).isEmpty();
        assertThat(payload.statuses()).isEmpty();
    }
}

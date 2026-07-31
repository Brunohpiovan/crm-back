-- Suporte a idempotencia do webhook do Twilio: cada MessageSid so pode ser processado uma vez.
-- O evento e' registrado somente apos o processamento (participante/protocolo/mensagem) ter
-- sido concluido com sucesso, para que falhas (ex.: timeout ao baixar midia) nao bloqueiem
-- reprocessamento em uma proxima tentativa do Twilio.
CREATE TABLE whatsapp_webhook_evento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_sid VARCHAR(64) NOT NULL,
    processado_em DATETIME NOT NULL,
    CONSTRAINT uk_whatsapp_webhook_evento_message_sid UNIQUE (message_sid)
);

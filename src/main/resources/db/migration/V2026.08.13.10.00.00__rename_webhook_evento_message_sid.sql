-- A tabela de idempotência do webhook deixa de ser específica da Twilio (MessageSid) e passa a
-- guardar o id de mensagem externo genérico (hoje: wamid da Meta). Mesmo mecanismo de idempotência
-- (registrar ANTES do processamento pesado) e mesma constraint UNIQUE, só o nome do provedor muda.
ALTER TABLE whatsapp_webhook_evento
    CHANGE COLUMN message_sid external_message_id VARCHAR(128) NOT NULL;

ALTER TABLE whatsapp_webhook_evento
    DROP INDEX uk_whatsapp_webhook_evento_message_sid,
    ADD CONSTRAINT uk_whatsapp_webhook_evento_external_message_id UNIQUE (external_message_id);

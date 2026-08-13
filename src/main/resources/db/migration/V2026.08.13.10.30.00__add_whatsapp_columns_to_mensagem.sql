-- Vincula cada Mensagem ao id de mensagem externo da Meta (wamid) e ao status de entrega
-- (sent/delivered/read/failed) reportado pelo webhook de status — nenhum dos dois existia no
-- fluxo Twilio (nunca havia rastreio de status, e o MessageSid só ficava na tabela de idempotência
-- do webhook, nunca na própria Mensagem). external_message_id fica NULL em mensagens antigas
-- (histórico pré-migração) e em mensagens sem envio via API (ex.: nenhuma hoje, mas mantido
-- nullable por segurança). UNIQUE aqui é best-effort: a idempotência de verdade continua sendo
-- feita via whatsapp_webhook_evento.
ALTER TABLE mensagem
    ADD COLUMN external_message_id VARCHAR(128) NULL,
    ADD COLUMN status VARCHAR(20) NULL,
    ADD CONSTRAINT uk_mensagem_external_message_id UNIQUE (external_message_id);

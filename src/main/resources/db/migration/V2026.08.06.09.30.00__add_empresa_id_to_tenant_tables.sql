-- Adiciona empresa_id (com DEFAULT 1 = Empresa Padrão) em toda tabela cuja entidade JPA passou
-- a ser @TenantId (ver TenantIdentifierResolver). MySQL 8 faz ADD COLUMN ... DEFAULT <constante>
-- só de metadado (instantâneo, sem reescrever a tabela) — FK e índice ficam na próxima migration,
-- que precisa validar linha por linha e por isso é separada.

-- Usuario e Participante: login/cpf deixam de ser únicos globalmente (próxima migration troca
-- pra único por empresa_id).
ALTER TABLE usuario ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE participante ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;

-- Funil/Etapa: pipeline de vendas.
ALTER TABLE funil ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE etapa ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;

-- Oportunidade/Protocolo: núcleo do CRM, já usados extensivamente pelo dashboard.
ALTER TABLE oportunidade ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE protocolo ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;

-- Tag: nome deixa de ser único globalmente (próxima migration troca pra único por empresa_id).
ALTER TABLE tag ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;

-- Templates de e-mail, equipes e grupos de chat interno.
ALTER TABLE template_email ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE equipe ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE chat_grupo ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;

-- Webhook do WhatsApp (message_sid continua globalmente único, é garantido pela própria Twilio).
ALTER TABLE whatsapp_webhook_evento ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;

-- Cadência de funil e sua auditoria de execução.
ALTER TABLE cadencia_funil ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE log_movimentacao_cadencia ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;

-- Mensagens de atendimento (chat externo) e chat interno.
ALTER TABLE mensagem ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE mensagem_interna ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;

-- Log de acesso (login) — dado sensível, uma empresa não pode ver quem logou em outra.
ALTER TABLE acesso ADD COLUMN empresa_id BIGINT NOT NULL DEFAULT 1;

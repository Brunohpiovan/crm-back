-- IDs publicos (UUID) pra parar de expor o id sequencial interno em URL/resposta de API
-- (protecao contra enumeracao/IDOR). O id numerico continua sendo a PK de verdade — nada
-- muda em FK/join/@TenantId; public_id e so um identificador alternativo pra uso externo.
-- Cada linha precisa de um UUID DIFERENTE (ao contrario do empresa_id do plano anterior,
-- que era a mesma constante) — por isso ADD COLUMN nullable primeiro, UPDATE com UUID()
-- linha a linha depois, e so na proxima migration NOT NULL + UNIQUE (que precisa validar
-- unicidade, custo diferente de metadado puro).

ALTER TABLE usuario ADD COLUMN public_id CHAR(36) NULL;
UPDATE usuario SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE funil ADD COLUMN public_id CHAR(36) NULL;
UPDATE funil SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE etapa ADD COLUMN public_id CHAR(36) NULL;
UPDATE etapa SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE oportunidade ADD COLUMN public_id CHAR(36) NULL;
UPDATE oportunidade SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE protocolo ADD COLUMN public_id CHAR(36) NULL;
UPDATE protocolo SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE tag ADD COLUMN public_id CHAR(36) NULL;
UPDATE tag SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE template_email ADD COLUMN public_id CHAR(36) NULL;
UPDATE template_email SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE equipe ADD COLUMN public_id CHAR(36) NULL;
UPDATE equipe SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE chat_grupo ADD COLUMN public_id CHAR(36) NULL;
UPDATE chat_grupo SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE participante ADD COLUMN public_id CHAR(36) NULL;
UPDATE participante SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE cadencia_funil ADD COLUMN public_id CHAR(36) NULL;
UPDATE cadencia_funil SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE mensagem ADD COLUMN public_id CHAR(36) NULL;
UPDATE mensagem SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE mensagem_interna ADD COLUMN public_id CHAR(36) NULL;
UPDATE mensagem_interna SET public_id = UUID() WHERE public_id IS NULL;

ALTER TABLE acesso ADD COLUMN public_id CHAR(36) NULL;
UPDATE acesso SET public_id = UUID() WHERE public_id IS NULL;

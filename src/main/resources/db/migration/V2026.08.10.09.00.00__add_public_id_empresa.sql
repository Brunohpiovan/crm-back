-- Empresa ficou de fora da migration V2026.08.06.11.00.00 (que deu public_id pra maioria das
-- tabelas). Endpoints /master/empresas/{id} e /master/empresas/{empresaId}/usuarios/** ainda
-- expunham o id sequencial na URL. Mesmo padrão das demais tabelas: coluna nullable + backfill
-- primeiro, NOT NULL + UNIQUE já na mesma migration (tabela pequena, sem custo de lock relevante).

ALTER TABLE empresa ADD COLUMN public_id CHAR(36) NULL;
UPDATE empresa SET public_id = UUID() WHERE public_id IS NULL;
ALTER TABLE empresa MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_empresa_public_id UNIQUE (public_id);

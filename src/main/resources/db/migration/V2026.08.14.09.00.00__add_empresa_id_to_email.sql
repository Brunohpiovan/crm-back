-- A tabela email ficou de fora da migration multi-tenant original (V2026.08.06.09.30.00 / 10.00.00)
-- - unica tabela de negocio sem empresa_id, o que permitiria, em qualquer endpoint futuro que
-- liste/busque Email sem filtrar explicitamente por remetente, vazar assunto/corpo/destinatario de
-- e-mails de uma empresa para usuarios de outra. Corrige a inconsistencia adicionando a mesma
-- coluna @TenantId que todas as outras entidades de negocio ja tem (ver TenantIdentifierResolver).

ALTER TABLE email ADD COLUMN empresa_id BIGINT NULL;

-- Backfill a partir da empresa do proprio remetente (sempre resolvivel: remetente_id e NOT NULL
-- e tem FK para usuario).
UPDATE email e
  INNER JOIN usuario u ON u.id = e.remetente_id
  SET e.empresa_id = u.empresa_id
  WHERE e.empresa_id IS NULL;

ALTER TABLE email
  MODIFY COLUMN empresa_id BIGINT NOT NULL,
  ADD CONSTRAINT fk_email_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
  ADD INDEX idx_email_empresa_id (empresa_id);

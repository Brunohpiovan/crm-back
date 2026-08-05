-- Converte usuario.cargo e template_email.situacao de valores ordinais (BIT, EnumType
-- ordinal implicito por falta de @Enumerated) para o nome do enum (EnumType.STRING),
-- preservando os dados existentes. Mesmo bug e mesmo padrao de correcao ja aplicados em
-- V2026.07.30.11 (tag) e V2026.07.30.14 (protocolo).
-- Mapeamento usado (ordem declarada hoje nos enums Java, nunca alterada):
--   UserRole:  0=ADMINISTRADOR, 1=VENDEDOR
--   Situacao:  0=ATIVA, 1=INATIVA

ALTER TABLE usuario DROP CHECK check_usuario_cargo;
ALTER TABLE template_email DROP CHECK check_template_email_situacao;

ALTER TABLE usuario
  ADD COLUMN cargo_str VARCHAR(20) NULL;
ALTER TABLE template_email
  ADD COLUMN situacao_str VARCHAR(20) NULL;

UPDATE usuario SET
  cargo_str = CASE CAST(cargo AS UNSIGNED)
    WHEN 0 THEN 'ADMINISTRADOR'
    WHEN 1 THEN 'VENDEDOR'
  END;

UPDATE template_email SET
  situacao_str = CASE CAST(situacao AS UNSIGNED)
    WHEN 0 THEN 'ATIVA'
    WHEN 1 THEN 'INATIVA'
  END;

ALTER TABLE usuario DROP COLUMN cargo;
ALTER TABLE template_email DROP COLUMN situacao;

ALTER TABLE usuario CHANGE COLUMN cargo_str cargo VARCHAR(20) NOT NULL;
ALTER TABLE template_email CHANGE COLUMN situacao_str situacao VARCHAR(20) NULL;

ALTER TABLE usuario
  ADD CONSTRAINT check_usuario_cargo CHECK (cargo IN ('ADMINISTRADOR', 'VENDEDOR'));
ALTER TABLE template_email
  ADD CONSTRAINT check_template_email_situacao CHECK (situacao IS NULL OR situacao IN ('ATIVA', 'INATIVA'));

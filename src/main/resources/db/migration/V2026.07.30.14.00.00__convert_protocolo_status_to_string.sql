-- Converte protocolo.status de valor ordinal (BIT, EnumType ordinal implicito por falta de
-- @Enumerated) para o nome do enum (EnumType.STRING), preservando os dados existentes.
-- Dados reais verificados antes desta migration: apenas os valores 0 e 1 estao presentes
-- (7 linhas com 0, 83 linhas com 1), sem valores invalidos.
-- Mapeamento usado (ordem declarada hoje no enum StatusProtocolo, nunca alterada):
--   Status: 0=ABERTO, 1=FECHADO

ALTER TABLE protocolo DROP CHECK check_protocolo_status;

ALTER TABLE protocolo
  ADD COLUMN status_str VARCHAR(20) NULL;

UPDATE protocolo SET
  status_str = CASE CAST(status AS UNSIGNED)
    WHEN 0 THEN 'ABERTO'
    WHEN 1 THEN 'FECHADO'
  END;

-- a coluna status era a unica do indice idx_protocolo_status: o DROP COLUMN abaixo remove
-- esse indice junto, por isso ele e recriado ao final sobre a nova coluna string.
ALTER TABLE protocolo DROP COLUMN status;

ALTER TABLE protocolo CHANGE COLUMN status_str status VARCHAR(20) NOT NULL;

ALTER TABLE protocolo
  ADD CONSTRAINT check_protocolo_status CHECK (status IN ('ABERTO', 'FECHADO'));

ALTER TABLE protocolo
  ADD INDEX idx_protocolo_status (status);

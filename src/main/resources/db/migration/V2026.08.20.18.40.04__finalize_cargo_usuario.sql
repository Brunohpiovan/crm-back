-- Etapa 5 de 5: fecha o modelo novo. Se o backfill (etapa 4) tiver deixado qualquer usuario sem
-- cargo_id, o MODIFY ... NOT NULL falha e a migration inteira aborta -- que e o comportamento
-- correto: melhor falhar aqui do que deixar um usuario sem cargo (e, portanto, sem autorizacao
-- calculavel) passar despercebido.
--
-- O CHECK precisa cair ANTES do DROP COLUMN: no MySQL 8 nao e possivel remover uma coluna
-- referenciada por uma check constraint.
ALTER TABLE usuario DROP CHECK check_usuario_cargo;

ALTER TABLE usuario MODIFY cargo_id BIGINT NOT NULL;
ALTER TABLE usuario ADD CONSTRAINT fk_usuario_cargo FOREIGN KEY (cargo_id) REFERENCES cargo(id);

ALTER TABLE usuario DROP COLUMN cargo;

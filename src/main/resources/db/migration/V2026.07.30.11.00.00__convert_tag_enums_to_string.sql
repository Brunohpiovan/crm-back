-- Converte tag.cor, tag.pertence e tag.situacao de valores ordinais (numericos)
-- para o nome do enum (EnumType.STRING), preservando os dados existentes.
-- Mapeamento usado (ordem declarada hoje nos enums Java, nunca alterada):
--   Cor:      0=VERMELHO, 1=VERDE, 2=AZUL, 3=AMARELO
--   Pertence: 0=OPORTUNIDADES, 1=PESSOAS
--   Situacao: 0=ATIVA, 1=INATIVA

ALTER TABLE tag DROP CHECK check_tag_cor;
ALTER TABLE tag DROP CHECK check_tag_pertence;
ALTER TABLE tag DROP CHECK check_tag_situacao;

ALTER TABLE tag
  ADD COLUMN cor_str VARCHAR(20) NULL,
  ADD COLUMN pertence_str VARCHAR(20) NULL,
  ADD COLUMN situacao_str VARCHAR(20) NULL;

UPDATE tag SET
  cor_str = CASE CAST(cor AS UNSIGNED)
    WHEN 0 THEN 'VERMELHO'
    WHEN 1 THEN 'VERDE'
    WHEN 2 THEN 'AZUL'
    WHEN 3 THEN 'AMARELO'
  END,
  pertence_str = CASE CAST(pertence AS UNSIGNED)
    WHEN 0 THEN 'OPORTUNIDADES'
    WHEN 1 THEN 'PESSOAS'
  END,
  situacao_str = CASE CAST(situacao AS UNSIGNED)
    WHEN 0 THEN 'ATIVA'
    WHEN 1 THEN 'INATIVA'
  END;

ALTER TABLE tag DROP COLUMN cor;
ALTER TABLE tag DROP COLUMN pertence;
ALTER TABLE tag DROP COLUMN situacao;

ALTER TABLE tag CHANGE COLUMN cor_str cor VARCHAR(20) NOT NULL;
ALTER TABLE tag CHANGE COLUMN pertence_str pertence VARCHAR(20) NULL;
ALTER TABLE tag CHANGE COLUMN situacao_str situacao VARCHAR(20) NULL;

ALTER TABLE tag
  ADD CONSTRAINT check_tag_cor CHECK (cor IN ('VERMELHO', 'VERDE', 'AZUL', 'AMARELO')),
  ADD CONSTRAINT check_tag_pertence CHECK (pertence IS NULL OR pertence IN ('OPORTUNIDADES', 'PESSOAS')),
  ADD CONSTRAINT check_tag_situacao CHECK (situacao IS NULL OR situacao IN ('ATIVA', 'INATIVA'));

CREATE TABLE tag (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  cor BIT(1) NULL,
  nome VARCHAR(150) NULL,
  pertence BIT(1) NULL,
  situacao BIT(1) NULL,

  CONSTRAINT unique_tag_nome UNIQUE (nome),
  CONSTRAINT check_tag_cor CHECK (cor BETWEEN 0 AND 3),
  CONSTRAINT check_tag_pertence CHECK (pertence BETWEEN 0 AND 1),
  CONSTRAINT check_tag_situacao CHECK (situacao BETWEEN 0 AND 1)
);

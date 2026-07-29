CREATE TABLE etapa (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(150) NOT NULL,
  funil_id BIGINT NOT NULL,
  posicao INT NULL,
  valor_total DECIMAL(38,2) NULL,

  CONSTRAINT fk_etapa_funil FOREIGN KEY (funil_id) REFERENCES funil(id)
);

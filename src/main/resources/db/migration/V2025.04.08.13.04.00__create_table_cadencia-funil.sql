CREATE TABLE cadencia_funil (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  descricao VARCHAR(255) NULL,
  dias_na_etapa INT NOT NULL,
  horario_movimentacao TIME NULL,
  nome VARCHAR(255) NOT NULL,
  situacao ENUM('ATIVA', 'INATIVA') NOT NULL,
  etapa_destino_id BIGINT NOT NULL,
  etapa_origem_id BIGINT NOT NULL,
  funil_destino_id BIGINT NOT NULL,
  funil_origem_id BIGINT NOT NULL,

  CONSTRAINT fk_cadencia_funil_etapa_destino FOREIGN KEY (etapa_destino_id) REFERENCES etapa (id),
  CONSTRAINT fk_cadencia_funil_etapa_origem FOREIGN KEY (etapa_origem_id) REFERENCES etapa (id),
  CONSTRAINT fk_cadencia_funil_funil_destino FOREIGN KEY (funil_destino_id) REFERENCES funil (id),
  CONSTRAINT fk_cadencia_funil_funil_origem FOREIGN KEY (funil_origem_id) REFERENCES funil (id)
);

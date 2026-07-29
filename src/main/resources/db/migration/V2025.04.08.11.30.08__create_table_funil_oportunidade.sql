CREATE TABLE oportunidade (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  card_id BIGINT NULL,
  cliente_id BIGINT NULL,
  criador_id BIGINT NULL,
  titulo VARCHAR(150) NULL,
  valor DECIMAL(38,2) NULL,
  data_criacao DATETIME NULL,
  url_anexo VARCHAR(255) NULL,
  indice INT NULL,
  origem ENUM('EMAIL','FACEBOOK','INSTAGRAM','OUTRO','WHATSAPP','LINKEDIN') NULL,
  interesse VARCHAR(100) NULL,
  descricao VARCHAR(150) NULL,

  CONSTRAINT fk_oportunidade_card FOREIGN KEY (card_id) REFERENCES etapa(id),
  CONSTRAINT fk_oportunidade_cliente FOREIGN KEY (cliente_id) REFERENCES participante(id),
  CONSTRAINT fk_oportunidade_criador FOREIGN KEY (criador_id) REFERENCES usuario(id),
  CONSTRAINT check_oportunidade_origem CHECK (origem IN ('EMAIL','FACEBOOK','INSTAGRAM','OUTRO','WHATSAPP','LINKEDIN'))
);

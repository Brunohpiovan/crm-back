CREATE TABLE mensagem (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  conteudo TEXT,
  data_envio DATETIME NULL,
  protocolo_id BIGINT NULL,
  sender_id BIGINT NULL,

  CONSTRAINT fk_mensagem_protocolo FOREIGN KEY (protocolo_id) REFERENCES protocolo(id),
  CONSTRAINT fk_mensagem_participante FOREIGN KEY (sender_id) REFERENCES participante(id)
);

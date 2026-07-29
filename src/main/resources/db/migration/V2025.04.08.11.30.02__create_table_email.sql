CREATE TABLE email (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  assunto VARCHAR(255) NULL,
  corpo TEXT,
  destinatario VARCHAR(255) NULL,
  remetente_id BIGINT NOT NULL,

  CONSTRAINT fk_email_usuario_remetente FOREIGN KEY (remetente_id) REFERENCES usuario(id)
);

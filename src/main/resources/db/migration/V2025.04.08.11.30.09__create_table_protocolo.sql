CREATE TABLE protocolo (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  data_criacao DATETIME NULL,
  data_encerramento DATETIME NULL,
  status BIT(1) NOT NULL,
  admin_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,

  CONSTRAINT fk_protocolo_admin_id FOREIGN KEY (admin_id) REFERENCES usuario (id),
  CONSTRAINT fk_protocolo_user_id FOREIGN KEY (user_id) REFERENCES participante (id),
  CONSTRAINT check_protocolo_status CHECK (status BETWEEN 0 AND 1)
);

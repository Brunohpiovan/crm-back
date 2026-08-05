CREATE TABLE equipe (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(150) NOT NULL,
  criado_em DATETIME NULL,
  atualizado_em DATETIME NULL
);

CREATE TABLE equipe_usuario (
  equipe_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,

  PRIMARY KEY (equipe_id, usuario_id),
  CONSTRAINT fk_equipe_usuario_equipe FOREIGN KEY (equipe_id) REFERENCES equipe(id),
  CONSTRAINT fk_equipe_usuario_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

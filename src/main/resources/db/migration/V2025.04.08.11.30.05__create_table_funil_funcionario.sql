CREATE TABLE funil_funcionarios (
  funil_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,

  PRIMARY KEY (funil_id, usuario_id),
  CONSTRAINT fk_funil_funcionarios_funil FOREIGN KEY (funil_id) REFERENCES funil(id),
  CONSTRAINT fk_funil_funcionarios_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

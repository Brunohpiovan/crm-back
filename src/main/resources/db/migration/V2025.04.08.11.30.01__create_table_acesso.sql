CREATE TABLE acesso (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  data_acesso DATETIME NOT NULL,
  data_saida DATETIME NULL,
  endereco_ip VARCHAR(45) NOT NULL,
  fuso_horario VARCHAR(50) NULL,
  localizacao VARCHAR(255) NULL,
  navegador VARCHAR(100) NULL,
  provedor_internet VARCHAR(100) NULL,
  sistema_operacional VARCHAR(100) NULL,
  tipo_dispositivo VARCHAR(50) NULL,
  versao_navegador VARCHAR(20) NULL,
  user_id BIGINT NOT NULL,

  CONSTRAINT fk_acesso_usuario FOREIGN KEY (user_id) REFERENCES usuario(id)
);

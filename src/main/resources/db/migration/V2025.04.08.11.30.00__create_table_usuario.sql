CREATE TABLE usuario (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bairro VARCHAR(100) NOT NULL,
  cargo BIT(1) NOT NULL,
  celular VARCHAR(15) NOT NULL,
  cidade VARCHAR(100) NOT NULL,
  complemento VARCHAR(255) NULL,
  cpf VARCHAR(14) NOT NULL,
  data_nascimento DATE NOT NULL,
  endereco VARCHAR(255) NOT NULL,
  login VARCHAR(150) NOT NULL,
  nome VARCHAR(150) NOT NULL,
  numero_residencial VARCHAR(50) NOT NULL,
  observacoes TEXT,
  rg VARCHAR(12) NOT NULL,
  senha VARCHAR(255) NOT NULL,
  uf ENUM(
    'AC','AL','AM','AP','BA','CE','DF','ES','GO','MA','MG','MS','MT',
    'PA','PB','PE','PI','PR','RJ','RN','RO','RR','RS','SC','SE','SP','TO'
  ) NOT NULL,
  url_picture VARCHAR(255) NULL,

  CONSTRAINT unique_usuario_cpf UNIQUE (cpf),
  CONSTRAINT unique_usuario_login UNIQUE (login),
  CONSTRAINT check_usuario_cargo CHECK (cargo BETWEEN 0 AND 1)
);

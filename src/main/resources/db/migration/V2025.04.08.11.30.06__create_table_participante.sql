CREATE TABLE participante (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  bairro VARCHAR(100) NULL,
  celular VARCHAR(15) NOT NULL,
  cidade VARCHAR(100) NULL,
  complemento VARCHAR(255) NULL,
  cpf VARCHAR(14) NULL,
  data_nascimento DATE NULL,
  endereco VARCHAR(255) NULL,
  login VARCHAR(150) NULL,
  nome VARCHAR(150) NOT NULL,
  numero_residencial VARCHAR(50) NULL,
  observacoes TEXT,
  rg VARCHAR(12) NULL,
  uf ENUM('AC','AL','AM','AP','BA','CE','DF','ES','GO','MA','MG','MS','MT','PA','PB','PE','PI','PR','RJ','RN','RO','RR','RS','SC','SE','SP','TO') NULL,
  url_picture VARCHAR(255) NULL,

  CONSTRAINT unique_participante_cpf UNIQUE (cpf),
  CONSTRAINT unique_participante_login UNIQUE (login)
);

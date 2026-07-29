CREATE TABLE template_email (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  assunto VARCHAR(255) NULL,
  mensagem TEXT,
  nome VARCHAR(255) NULL,
  url_anexo VARCHAR(255) NULL,
  situacao BIT(1) NULL,

  CONSTRAINT check_template_email_situacao CHECK (situacao BETWEEN 0 AND 1)
);

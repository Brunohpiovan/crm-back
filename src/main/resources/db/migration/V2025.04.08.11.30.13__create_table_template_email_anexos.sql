CREATE TABLE template_email_anexos (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  template_email_id BIGINT NOT NULL,
  url_anexo VARCHAR(255) NULL,

  CONSTRAINT fk_template_email_anexos_template_email FOREIGN KEY (template_email_id) REFERENCES template_email(id)
);


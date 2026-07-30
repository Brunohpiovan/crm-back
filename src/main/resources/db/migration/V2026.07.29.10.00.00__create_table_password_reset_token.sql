CREATE TABLE password_reset_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(100) NOT NULL,
    usuario_id BIGINT NOT NULL,
    criado_em DATETIME NOT NULL,
    expira_em DATETIME NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_password_reset_token_token UNIQUE (token),
    CONSTRAINT fk_password_reset_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

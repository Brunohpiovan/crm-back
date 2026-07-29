CREATE TABLE chat_grupo_usuario (
    chat_grupo_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    PRIMARY KEY (chat_grupo_id, usuario_id),
    CONSTRAINT fk_chat_grupo_usuario_chat_grupo
        FOREIGN KEY (chat_grupo_id)
        REFERENCES chat_grupo (id),
    CONSTRAINT fk_chat_grupo_usuario_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuario (id)
);

CREATE TABLE mensagem_interna (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chat_grupo_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    conteudo TEXT NOT NULL,
    data_envio DATETIME NOT NULL,
    CONSTRAINT fk_mensagem_interna_chat_grupo
        FOREIGN KEY (chat_grupo_id)
        REFERENCES chat_grupo (id),
    CONSTRAINT fk_mensagem_interna_sender
        FOREIGN KEY (sender_id)
        REFERENCES usuario (id)
);

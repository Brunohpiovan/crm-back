-- Comentários de uma oportunidade, exibidos no modal de edição ("Comentários"), com autor real
-- (FK para usuario, ao contrário do histórico que guarda snapshot do nome) para que nome/avatar
-- fiquem sempre atualizados e para permitir checar quem pode excluir cada comentário. Anexo
-- opcional (mesmo esquema de upload usado no anexo da própria oportunidade). ON DELETE CASCADE
-- em oportunidade_id pelo mesmo motivo do histórico: exclusão física da oportunidade não deixa
-- nenhuma tela que ainda exiba esses comentários depois.
CREATE TABLE oportunidade_comentario (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL UNIQUE,
    empresa_id BIGINT NOT NULL,
    oportunidade_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    conteudo TEXT NOT NULL,
    url_anexo VARCHAR(500) NULL,
    criado_em DATETIME NOT NULL,
    CONSTRAINT fk_oportunidade_comentario_oportunidade FOREIGN KEY (oportunidade_id) REFERENCES oportunidade(id) ON DELETE CASCADE,
    CONSTRAINT fk_oportunidade_comentario_autor FOREIGN KEY (autor_id) REFERENCES usuario(id)
);

CREATE INDEX idx_oportunidade_comentario_oportunidade ON oportunidade_comentario(oportunidade_id, criado_em);

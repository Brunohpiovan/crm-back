-- Vínculo N:N entre Oportunidade e Processo. ON DELETE CASCADE em oportunidade_id (mesmo padrão de
-- oportunidade_comentario): excluir a oportunidade não deixa vínculo órfão. Sem cascade em
-- processo_id — desvincular (DELETE nesta tabela) nunca apaga o Processo (outras oportunidades ou
-- a tela /processos podem continuar referenciando-o).
CREATE TABLE oportunidade_processo (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    oportunidade_id BIGINT NOT NULL,
    processo_id BIGINT NOT NULL,
    vinculado_em DATETIME NOT NULL,
    vinculado_por BIGINT NOT NULL,
    CONSTRAINT uk_oportunidade_processo UNIQUE (oportunidade_id, processo_id),
    CONSTRAINT fk_oportunidade_processo_oportunidade FOREIGN KEY (oportunidade_id) REFERENCES oportunidade(id) ON DELETE CASCADE,
    CONSTRAINT fk_oportunidade_processo_processo FOREIGN KEY (processo_id) REFERENCES processo(id),
    CONSTRAINT fk_oportunidade_processo_usuario FOREIGN KEY (vinculado_por) REFERENCES usuario(id)
);

CREATE INDEX idx_oportunidade_processo_processo ON oportunidade_processo(processo_id);

-- Cada publicacao/aparicao encontrada em Diario Oficial para uma OAB monitorada (analogo a
-- processo_documento, mas para o callback de diario_movimentacao_nova/diario_citacao_nova em vez
-- de novo_documento). chave_dedupe e a natural key (monitoramento_id+diario_id+pagina) que
-- sobrevive a reentrega do callback (ate 11x) -- ver IntimacaoService.registrarDoCallback.
CREATE TABLE intimacao (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    intimacao_monitoramento_id BIGINT NOT NULL,
    processo_id BIGINT NULL,
    numero_cnj_identificado VARCHAR(25) NULL,
    diario_nome VARCHAR(255) NULL,
    diario_sigla VARCHAR(20) NULL,
    diario_data DATE NULL,
    conteudo TEXT NULL,
    link VARCHAR(500) NULL,
    chave_dedupe VARCHAR(255) NOT NULL,
    lida_em DATETIME NULL,
    criado_em DATETIME NOT NULL,
    CONSTRAINT uk_intimacao_dedupe UNIQUE (chave_dedupe),
    CONSTRAINT fk_intimacao_monitoramento FOREIGN KEY (intimacao_monitoramento_id) REFERENCES intimacao_monitoramento(id) ON DELETE CASCADE,
    CONSTRAINT fk_intimacao_processo FOREIGN KEY (processo_id) REFERENCES processo(id)
);

CREATE INDEX idx_intimacao_monitoramento_criado ON intimacao (intimacao_monitoramento_id, criado_em);
CREATE INDEX idx_intimacao_monitoramento_lida ON intimacao (intimacao_monitoramento_id, lida_em);

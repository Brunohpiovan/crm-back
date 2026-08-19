-- Documentos públicos de um processo (busca manual sob demanda ou callback de monitoramento
-- novo_documento). UNIQUE(processo_id, escavador_documento_id) é o que torna
-- ProcessoDocumentoService idempotente entre os dois caminhos, mesmo racional de
-- uk_processo_movimentacao_dedupe (o callback novo_documento pode chegar duplicado, até 11x).
CREATE TABLE processo_documento (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    escavador_documento_id VARCHAR(255) NOT NULL,
    titulo VARCHAR(255) NULL,
    descricao TEXT NULL,
    data_documento DATE NULL,
    tipo VARCHAR(60) NULL,
    extensao_arquivo VARCHAR(20) NULL,
    quantidade_paginas INT NULL,
    fonte VARCHAR(30) NOT NULL,
    criado_em DATETIME NOT NULL,
    CONSTRAINT fk_processo_documento_processo FOREIGN KEY (processo_id) REFERENCES processo(id) ON DELETE CASCADE,
    CONSTRAINT uk_processo_documento_dedupe UNIQUE (processo_id, escavador_documento_id)
);

CREATE INDEX idx_processo_documento_processo ON processo_documento(processo_id, data_documento DESC);

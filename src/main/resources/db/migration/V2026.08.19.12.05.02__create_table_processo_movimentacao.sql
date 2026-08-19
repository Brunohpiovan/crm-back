-- Linha do tempo processual. UNIQUE(processo_id, data_movimentacao, hash_conteudo) é o que torna
-- ProcessoMovimentacaoService.registrarMovimentacoes idempotente entre consulta manual
-- (ATUALIZACAO_MANUAL) e callback de monitoramento do Prompt 2 (CALLBACK_MONITORAMENTO) — ambos
-- os caminhos passam pelo mesmo método e nunca duplicam a mesma movimentação. hash_conteudo é
-- SHA-256 (hex, 64 chars) do texto de conteudo normalizado, calculado na aplicação.
CREATE TABLE processo_movimentacao (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    data_movimentacao DATETIME NOT NULL,
    tipo VARCHAR(60) NULL,
    conteudo TEXT NOT NULL,
    hash_conteudo VARCHAR(64) NOT NULL,
    fonte VARCHAR(30) NOT NULL,
    criado_em DATETIME NOT NULL,
    CONSTRAINT fk_processo_movimentacao_processo FOREIGN KEY (processo_id) REFERENCES processo(id) ON DELETE CASCADE,
    CONSTRAINT uk_processo_movimentacao_dedupe UNIQUE (processo_id, data_movimentacao, hash_conteudo)
);

CREATE INDEX idx_processo_movimentacao_processo ON processo_movimentacao(processo_id, data_movimentacao DESC);

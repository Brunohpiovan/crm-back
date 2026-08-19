-- Assinatura de monitoramento continuo de um processo na Escavador (conta unica do juriq-crm).
-- UNIQUE(processo_id) sem filtro: MySQL nao suporta indice parcial (WHERE ativo = true), entao
-- reativar um monitoramento desligado atualiza a linha existente em vez de inserir outra -- ver
-- ProcessoMonitoramento.ativar(). escavador_monitoramento_id fica NULL enquanto a criacao da
-- assinatura nao foi confirmada; e essa condicao (ativo = true AND id IS NULL) que o
-- EscavadorMonitoramentoScheduler reconcilia.
CREATE TABLE processo_monitoramento (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    processo_id BIGINT NOT NULL,
    frequencia VARCHAR(20) NOT NULL,
    com_documentos BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    escavador_monitoramento_id VARCHAR(40) NULL,
    ativado_em DATETIME NOT NULL,
    ativado_por BIGINT NULL,
    desativado_em DATETIME NULL,
    CONSTRAINT uk_processo_monitoramento_processo UNIQUE (processo_id),
    CONSTRAINT fk_processo_monitoramento_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_processo_monitoramento_processo FOREIGN KEY (processo_id) REFERENCES processo(id),
    CONSTRAINT fk_processo_monitoramento_usuario FOREIGN KEY (ativado_por) REFERENCES usuario(id)
);

-- Suporta a checagem de cota (countByEmpresaIdAndAtivoTrue), feita a cada ativacao.
CREATE INDEX idx_processo_monitoramento_empresa_ativo ON processo_monitoramento (empresa_id, ativo);

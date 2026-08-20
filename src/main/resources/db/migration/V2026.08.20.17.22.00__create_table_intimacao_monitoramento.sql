-- Assinatura de monitoramento continuo de uma OAB nos Diarios Oficiais (Escavador API v1,
-- POST /api/v1/monitoramentos, tipo=TERMO). Mesma "assinatura pendente ate confirmacao" de
-- processo_monitoramento: escavador_monitoramento_id fica NULL ate a criacao ser confirmada, e
-- essa condicao (ativo = true AND id IS NULL) que o IntimacaoMonitoramentoScheduler reconcilia.
-- Diferente de processo_monitoramento, o UNIQUE aqui e por OAB (nao por processo): um escritorio
-- com N advogados cadastra N linhas, uma por OAB.
CREATE TABLE intimacao_monitoramento (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    oab_numero VARCHAR(20) NOT NULL,
    oab_uf VARCHAR(2) NOT NULL,
    usuario_id BIGINT NULL,
    origem_ids TEXT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    escavador_monitoramento_id VARCHAR(40) NULL,
    ativado_em DATETIME NOT NULL,
    ativado_por BIGINT NULL,
    desativado_em DATETIME NULL,
    CONSTRAINT uk_intimacao_monitoramento_oab UNIQUE (empresa_id, oab_numero, oab_uf),
    CONSTRAINT fk_intimacao_monitoramento_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_intimacao_monitoramento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_intimacao_monitoramento_ativado_por FOREIGN KEY (ativado_por) REFERENCES usuario(id)
);

-- Suporta a checagem de cota (countByAtivoTrue), feita a cada ativacao.
CREATE INDEX idx_intimacao_monitoramento_empresa_ativo ON intimacao_monitoramento (empresa_id, ativo);

-- Processo judicial consultado/vinculado via Escavador Business API (v2, conta única e
-- compartilhada do juriq-crm). numero_cnj é único por empresa (não globalmente): duas empresas
-- clientes podem, de forma legítima, consultar/acompanhar o mesmo processo público. Ver
-- ProcessoRepository.findByNumeroCnjIgnoringTenant (Prompt 2, casamento de callback sem tenant).
CREATE TABLE processo (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    public_id VARCHAR(36) NOT NULL UNIQUE,
    empresa_id BIGINT NOT NULL,
    numero_cnj VARCHAR(25) NOT NULL,
    tribunal VARCHAR(20) NULL,
    instancia VARCHAR(60) NULL,
    situacao VARCHAR(20) NOT NULL DEFAULT 'DESCONHECIDO',
    valor_causa DECIMAL(15,2) NULL,
    data_distribuicao DATE NULL,
    area VARCHAR(100) NULL,
    assunto VARCHAR(255) NULL,
    resumo_ia TEXT NULL,
    resumo_ia_gerado_em DATETIME NULL,
    ultima_consulta_em DATETIME NULL,
    criado_em DATETIME NOT NULL,
    atualizado_em DATETIME NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_processo_empresa_numero_cnj UNIQUE (empresa_id, numero_cnj),
    CONSTRAINT fk_processo_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

CREATE INDEX idx_processo_numero_cnj ON processo(numero_cnj);

-- Ledger interno de consumo da API da Escavador por empresa. Mecanismo de seguranca/auditoria
-- contra gasto descontrolado por bug -- NAO e a unidade vendida ao cliente (essa e a cota de
-- processos monitorados, em empresa.processos_monitorados_limite) e nao aparece assim na UI.
-- Sem FK/NOT NULL em empresa_id: e alimentado por evento, inclusive de chamadas que ocorrem sem
-- tenant resolvido (scheduler), e perder o lancamento seria pior que grava-lo sem dono.
CREATE TABLE escavador_credito_lancamento (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NULL,
    endpoint VARCHAR(255) NOT NULL,
    custo_centavos INT NOT NULL DEFAULT 0,
    sucesso BOOLEAN NOT NULL,
    criado_em DATETIME NOT NULL
);

CREATE INDEX idx_escavador_credito_lancamento_empresa_data ON escavador_credito_lancamento (empresa_id, criado_em);

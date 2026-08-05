-- Log de auditoria de execução de cadência: só recebe uma linha quando o
-- MovimentacaoScheduler/CadenciaFunilService move uma oportunidade automaticamente — nunca em
-- movimentação manual (drag-and-drop). Substitui a aproximação anterior de "execucoesHoje" no
-- dashboard (que contava qualquer entrada na etapa destino, incluindo movimentação manual).
CREATE TABLE log_movimentacao_cadencia (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cadencia_funil_id BIGINT NOT NULL,
    oportunidade_id BIGINT NOT NULL,
    etapa_origem_id BIGINT NOT NULL,
    etapa_destino_id BIGINT NOT NULL,
    executado_em DATETIME NOT NULL,
    CONSTRAINT fk_log_movimentacao_cadencia_cadencia FOREIGN KEY (cadencia_funil_id) REFERENCES cadencia_funil(id),
    CONSTRAINT fk_log_movimentacao_cadencia_oportunidade FOREIGN KEY (oportunidade_id) REFERENCES oportunidade(id)
);

CREATE INDEX idx_log_movimentacao_cadencia_executado_em ON log_movimentacao_cadencia(executado_em);

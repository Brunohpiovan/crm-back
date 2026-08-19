-- Partes/advogados de um Processo (capa da Escavador §8.10 "Envolvido"). ON DELETE CASCADE: um
-- envolvido nunca existe sem o processo que o originou; reconsulta (upsert) apaga e regrava.
CREATE TABLE processo_envolvido (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    nome VARCHAR(255) NULL,
    tipo VARCHAR(20) NOT NULL,
    documento VARCHAR(32) NULL,
    oab VARCHAR(20) NULL,
    CONSTRAINT fk_processo_envolvido_processo FOREIGN KEY (processo_id) REFERENCES processo(id) ON DELETE CASCADE
);

CREATE INDEX idx_processo_envolvido_processo ON processo_envolvido(processo_id);

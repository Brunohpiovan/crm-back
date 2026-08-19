-- Log bruto de auditoria dos callbacks recebidos da Escavador. Sem empresa_id de proposito: o
-- webhook e publico e o tenant so e conhecido depois de resolver o processo pelo numero CNJ --
-- um callback para um processo inexistente nesta instalacao tambem precisa ficar registrado.
CREATE TABLE escavador_callback_evento (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recebido_em DATETIME NOT NULL,
    payload_json TEXT NOT NULL,
    processado BOOLEAN NOT NULL DEFAULT FALSE,
    erro TEXT NULL,
    numero_cnj_resolvido VARCHAR(25) NULL
);

-- Consulta tipica de diagnostico: "o que falhou nas ultimas horas".
CREATE INDEX idx_escavador_callback_evento_processado ON escavador_callback_evento (processado, recebido_em);

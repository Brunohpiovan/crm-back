-- Lock otimista para Protocolo: transferencia (encaminha) e fechamento (closeProtocolo)
-- concorrentes por atendentes diferentes hoje resultam em last-write-wins silencioso, sem
-- deteccao de conflito. Mesmo padrao ja usado em Etapa (V2026.07.30.17) e Oportunidade
-- (V2026.07.30.12).
ALTER TABLE protocolo
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

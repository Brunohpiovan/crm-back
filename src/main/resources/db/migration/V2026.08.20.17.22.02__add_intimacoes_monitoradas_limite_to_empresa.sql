-- Cota do plano: quantas OABs a empresa pode manter monitoradas simultaneamente (cada OAB ativa
-- consome uma vaga, mesmo espirito de processos_monitorados_limite). NULL = ilimitado. Nullable
-- tambem para nao exigir backfill: empresas existentes seguem sem limite ate o plano ser definido
-- (ver comentario no prompt sobre avisar o time de produto para um limite padrao por plano).
ALTER TABLE empresa ADD COLUMN intimacoes_monitoradas_limite INT NULL;

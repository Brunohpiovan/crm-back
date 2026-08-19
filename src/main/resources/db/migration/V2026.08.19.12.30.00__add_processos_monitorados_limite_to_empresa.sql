-- Cota do plano: quantos processos a empresa pode manter monitorados simultaneamente. NULL =
-- ilimitado (mesmo espirito de protocolo_risco_horas, configurado por empresa). Nullable tambem
-- para nao exigir backfill: empresas existentes seguem sem limite ate o plano ser definido.
ALTER TABLE empresa ADD COLUMN processos_monitorados_limite INT NULL;

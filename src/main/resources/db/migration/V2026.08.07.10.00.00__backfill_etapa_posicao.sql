UPDATE etapa e
JOIN (
  SELECT id, ROW_NUMBER() OVER (PARTITION BY funil_id ORDER BY id) - 1 AS rn
  FROM etapa
) ranked ON ranked.id = e.id
SET e.posicao = ranked.rn
WHERE e.posicao IS NULL;

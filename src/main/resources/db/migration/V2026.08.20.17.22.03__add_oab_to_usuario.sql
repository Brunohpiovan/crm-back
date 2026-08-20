-- Vinculo opcional entre um usuario (advogado do escritorio) e a OAB monitorada em
-- intimacao_monitoramento -- ver IntimacaoMonitoramento.usuarioId. Aditiva, nenhuma outra coluna
-- tocada; ver comentario de integracao em docs/features/prompt-1-busca-intimacoes.md sobre o
-- Prompt 2 (refatoracao de cargos/permissoes) rodando em paralelo sobre este mesmo arquivo.
ALTER TABLE usuario ADD COLUMN oab_numero VARCHAR(20) NULL;
ALTER TABLE usuario ADD COLUMN oab_uf VARCHAR(2) NULL;

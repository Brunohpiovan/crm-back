-- Marca um processo com uma solicitação de resumo IA em andamento na Escavador, para o
-- EscavadorResumoIaScheduler encontrar (sem filtro de tenant, roda em background) e notificar via
-- WebSocket quando concluir — sem isso, o usuário só sabe que terminou se estiver com a tela do
-- processo aberta e fizer polling manual.
ALTER TABLE processo ADD COLUMN resumo_ia_pendente BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_processo_resumo_ia_pendente ON processo(resumo_ia_pendente);

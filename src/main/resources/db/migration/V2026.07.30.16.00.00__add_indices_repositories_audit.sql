-- Indices resultantes da auditoria completa dos repositories (fetch types, N+1, paginacao).
-- Cada indice abaixo esta documentado com a query que ele beneficia.

-- participante.celular nao tem indice dedicado (apenas cpf/login sao UNIQUE) e e' a coluna mais
-- consultada do fluxo de WhatsApp: ParticipanteRepository.findByCelular e' chamado a cada mensagem
-- recebida no webhook (WhatsAppService) e na criacao de oportunidade (OportunidadeService.resolveCliente).
ALTER TABLE participante
    ADD INDEX idx_participante_celular (celular);

-- cadencia_funil.situacao e' filtrada por CadenciaFunilRepository.findAllBySituacao a cada execucao
-- do scheduler de movimentacao automatica (MovimentacaoScheduler, a cada minuto).
ALTER TABLE cadencia_funil
    ADD INDEX idx_cadencia_funil_situacao (situacao);

-- oportunidade.situacao e' usada em OportunidadeRepository.findCardsByEtapaIdInAndSituacaoIn e
-- ...AndTagIdsIn (filtro de funil por situacao, POST /funil/filtro).
ALTER TABLE oportunidade
    ADD INDEX idx_oportunidade_situacao (situacao);

-- OportunidadeRepository.findElegiveisParaMovimentacao (scheduler de cadencia, a cada minuto) filtra
-- por card_id (etapa de origem) e compara data_entrada_etapa <= :dataLimite: indice composto na
-- mesma ordem do WHERE evita varrer todas as oportunidades da etapa a cada execucao.
ALTER TABLE oportunidade
    ADD INDEX idx_oportunidade_card_data_entrada (card_id, data_entrada_etapa);

-- MensagemInternaRepository.findByChatGrupo pagina por chat_grupo_id ordenando por id DESC
-- (Sort.by("id").descending() em MensagemInternaService.getMessagesForProtocolLimit), mesmo padrao
-- que motivou idx_mensagem_protocolo_id_id na tabela mensagem (V2026.07.30.13.00.00).
ALTER TABLE mensagem_interna
    ADD INDEX idx_mensagem_interna_chat_grupo_id_id (chat_grupo_id, id);

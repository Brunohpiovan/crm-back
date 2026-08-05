-- Indices faltantes identificados na auditoria de performance (etapa 2), cruzando queries reais
-- com o schema atual. Todos de baixo risco de escrita (nenhuma dessas tabelas e hot-path de
-- altissima frequencia de update).

-- Usado em ~5 agregacoes do dashboard (sumValorPorSituacao, countPorSituacao, funilPorEtapa,
-- leadsPorOrigem, rankingOportunidadesPorUsuario), sempre situacao + data_criacao juntos.
CREATE INDEX idx_oportunidade_situacao_data_criacao ON oportunidade(situacao, data_criacao);

-- Usado em countAbertos, countEmRisco, avgTempoAtendimentoMinutos, rankingProtocolosPorUsuario,
-- countAbertosPorDia.
CREATE INDEX idx_protocolo_data_criacao ON protocolo(data_criacao);

-- Usado em countFechadosPorDia (status = 'FECHADO' AND data_encerramento BETWEEN ...).
CREATE INDEX idx_protocolo_status_data_encerramento ON protocolo(status, data_encerramento);

-- Log de auditoria do scheduler de cadencia (roda a cada minuto), filtrado por
-- etapa_destino_id + executado_em em countByExecutadoEmBetweenAndEtapaDestinoIdIn.
CREATE INDEX idx_log_movimentacao_etapa_destino_executado ON log_movimentacao_cadencia(etapa_destino_id, executado_em);

-- Tabelas de juncao com PK composta (parent_id, usuario_id): o indice da PK so cobre buscas pelo
-- prefixo esquerdo, nao por usuario_id isolado (2a coluna), que e como varias queries filtram.
CREATE INDEX idx_funil_funcionarios_usuario ON funil_funcionarios(usuario_id);
CREATE INDEX idx_chat_grupo_usuario_usuario ON chat_grupo_usuario(usuario_id);

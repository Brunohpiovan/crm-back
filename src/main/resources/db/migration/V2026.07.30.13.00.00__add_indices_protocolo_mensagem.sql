-- admin_id, user_id (protocolo) e protocolo_id, sender_id (mensagem) já possuem índice
-- implícito criado automaticamente pelo InnoDB para as FKs existentes; não duplicados aqui.

-- status não tem FK e é usado em toda busca de protocolo aberto (findByCelularAndStatus,
-- findByParticipanteIdAndStatusAberto, findByAdminCelularAndParticipanteCelularAndStatus).
ALTER TABLE protocolo
    ADD INDEX idx_protocolo_status (status);

-- a listagem paginada de mensagens filtra por protocolo_id e ordena por id (Sort.by("id")):
-- índice composto evita filesort nesse caminho, usado diariamente no chat de atendimento.
ALTER TABLE mensagem
    ADD INDEX idx_mensagem_protocolo_id_id (protocolo_id, id);

-- data_envio não tem FK/índice e é usada para ordenação cronológica do histórico de mensagens.
ALTER TABLE mensagem
    ADD INDEX idx_mensagem_data_envio (data_envio);

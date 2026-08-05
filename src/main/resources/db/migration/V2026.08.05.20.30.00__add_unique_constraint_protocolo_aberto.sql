-- Impede, no nivel de banco, que o mesmo participante (user_id) tenha mais de um protocolo
-- com status = 'ABERTO' simultaneamente. A checagem que ja existia em
-- ProtocoloService.createProtocolo (findByParticipanteIdAndStatusAberto antes do insert) e um
-- check-then-act sem lock: duas requisicoes concorrentes para o mesmo participante podiam passar
-- pela checagem antes de qualquer uma persistir, criando dois protocolos abertos. A coluna
-- gerada abaixo so recebe o user_id quando status='ABERTO' (fica NULL nos demais casos, e MySQL
-- permite varios NULLs sob indice UNIQUE), entao o indice unico cobre exatamente esse cenario
-- sem impedir o historico de protocolos ja fechados do mesmo participante.
ALTER TABLE protocolo
  ADD COLUMN participante_aberto_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN status = 'ABERTO' THEN user_id ELSE NULL END) VIRTUAL;

ALTER TABLE protocolo
  ADD UNIQUE INDEX uk_protocolo_participante_aberto (participante_aberto_id);

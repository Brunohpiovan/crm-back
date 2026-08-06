-- FK empresa_id -> empresa(id) + índice por tabela (toda query tenant-scoped filtra por essa
-- coluna) para as 16 tabelas que ganharam empresa_id na migration anterior. Separada da anterior
-- porque validar a FK linha a linha tem custo/lock diferente de um ADD COLUMN com DEFAULT.

ALTER TABLE usuario ADD CONSTRAINT fk_usuario_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_usuario_empresa_id (empresa_id);
ALTER TABLE participante ADD CONSTRAINT fk_participante_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_participante_empresa_id (empresa_id);
ALTER TABLE funil ADD CONSTRAINT fk_funil_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_funil_empresa_id (empresa_id);
ALTER TABLE etapa ADD CONSTRAINT fk_etapa_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_etapa_empresa_id (empresa_id);
ALTER TABLE oportunidade ADD CONSTRAINT fk_oportunidade_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_oportunidade_empresa_id (empresa_id);
ALTER TABLE protocolo ADD CONSTRAINT fk_protocolo_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_protocolo_empresa_id (empresa_id);
ALTER TABLE tag ADD CONSTRAINT fk_tag_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_tag_empresa_id (empresa_id);
ALTER TABLE template_email ADD CONSTRAINT fk_template_email_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_template_email_empresa_id (empresa_id);
ALTER TABLE equipe ADD CONSTRAINT fk_equipe_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_equipe_empresa_id (empresa_id);
ALTER TABLE chat_grupo ADD CONSTRAINT fk_chat_grupo_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_chat_grupo_empresa_id (empresa_id);
ALTER TABLE whatsapp_webhook_evento ADD CONSTRAINT fk_whatsapp_webhook_evento_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_whatsapp_webhook_evento_empresa_id (empresa_id);
ALTER TABLE cadencia_funil ADD CONSTRAINT fk_cadencia_funil_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_cadencia_funil_empresa_id (empresa_id);
ALTER TABLE log_movimentacao_cadencia ADD CONSTRAINT fk_log_movimentacao_cadencia_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_log_movimentacao_cadencia_empresa_id (empresa_id);
ALTER TABLE mensagem ADD CONSTRAINT fk_mensagem_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_mensagem_empresa_id (empresa_id);
ALTER TABLE mensagem_interna ADD CONSTRAINT fk_mensagem_interna_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_mensagem_interna_empresa_id (empresa_id);
ALTER TABLE acesso ADD CONSTRAINT fk_acesso_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id), ADD INDEX idx_acesso_empresa_id (empresa_id);

-- login/cpf deixam de ser únicos globalmente e passam a ser únicos por empresa (decisão de
-- produto: duas empresas podem ter funcionário/contato com o mesmo e-mail ou CPF). Nomes das
-- constraints antigas conferidos nas migrations originais (create_table_usuario/participante/tag).
ALTER TABLE usuario DROP INDEX unique_usuario_login, DROP INDEX unique_usuario_cpf,
  ADD CONSTRAINT uk_usuario_empresa_login UNIQUE (empresa_id, login),
  ADD CONSTRAINT uk_usuario_empresa_cpf UNIQUE (empresa_id, cpf);

ALTER TABLE participante DROP INDEX unique_participante_login, DROP INDEX unique_participante_cpf,
  ADD CONSTRAINT uk_participante_empresa_login UNIQUE (empresa_id, login),
  ADD CONSTRAINT uk_participante_empresa_cpf UNIQUE (empresa_id, cpf);

ALTER TABLE tag DROP INDEX unique_tag_nome,
  ADD CONSTRAINT uk_tag_empresa_nome UNIQUE (empresa_id, nome);

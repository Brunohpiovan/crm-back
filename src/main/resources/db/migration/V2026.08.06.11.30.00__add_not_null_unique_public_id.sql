-- Trava public_id como NOT NULL + UNIQUE depois do backfill da migration anterior. Separada
-- porque validar unicidade linha a linha tem custo/lock diferente de um ADD COLUMN simples.

ALTER TABLE usuario MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_usuario_public_id UNIQUE (public_id);
ALTER TABLE funil MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_funil_public_id UNIQUE (public_id);
ALTER TABLE etapa MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_etapa_public_id UNIQUE (public_id);
ALTER TABLE oportunidade MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_oportunidade_public_id UNIQUE (public_id);
ALTER TABLE protocolo MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_protocolo_public_id UNIQUE (public_id);
ALTER TABLE tag MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_tag_public_id UNIQUE (public_id);
ALTER TABLE template_email MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_template_email_public_id UNIQUE (public_id);
ALTER TABLE equipe MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_equipe_public_id UNIQUE (public_id);
ALTER TABLE chat_grupo MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_chat_grupo_public_id UNIQUE (public_id);
ALTER TABLE participante MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_participante_public_id UNIQUE (public_id);
ALTER TABLE cadencia_funil MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_cadencia_funil_public_id UNIQUE (public_id);
ALTER TABLE mensagem MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_mensagem_public_id UNIQUE (public_id);
ALTER TABLE mensagem_interna MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_mensagem_interna_public_id UNIQUE (public_id);
ALTER TABLE acesso MODIFY COLUMN public_id CHAR(36) NOT NULL, ADD CONSTRAINT uk_acesso_public_id UNIQUE (public_id);

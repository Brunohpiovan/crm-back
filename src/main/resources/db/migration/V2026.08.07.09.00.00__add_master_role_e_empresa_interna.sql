-- Usuário master (super-admin multi-empresa): administra o CRUD de empresas e os usuários de
-- qualquer empresa via /master/**, sem acesso às telas normais do CRM (chat, funil, etc. — cargo
-- MASTER só recebe a authority ROLE_MASTER, ver Usuario.getAuthorities()).
--
-- Login exige sempre codigoEmpresa + login (AuthenticationDTO), então o master também precisa
-- pertencer a uma empresa. Em vez de usar a empresa-padrao (que é uma empresa-cliente comum),
-- criamos uma empresa interna dedicada, marcada com `interna = TRUE` pra ficar de fora de
-- qualquer listagem/CRUD de empresas em /master/empresas (ver EmpresaService).
--
-- Sem INSERT em `participante` pro master: ele nunca acessa o chat interno, então não precisa do
-- espelhamento que UsuarioService.save() faz pros usuários normais.

ALTER TABLE usuario DROP CHECK check_usuario_cargo;
ALTER TABLE usuario ADD CONSTRAINT check_usuario_cargo CHECK (cargo IN ('ADMINISTRADOR', 'VENDEDOR', 'MASTER'));

ALTER TABLE empresa ADD COLUMN interna BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO empresa (
  codigo, nome, logo_url, timezone,
  protocolo_risco_horas, notificacao_visual_habilitada, notificacao_sonora_habilitada,
  interna, criado_em, atualizado_em
) VALUES (
  'juridiqsystem-admin',
  'JuridiqSystem - Administração Interna',
  NULL,
  'America/Sao_Paulo',
  24,
  TRUE,
  TRUE,
  TRUE,
  NOW(), NOW()
);

SET @empresa_master_id = LAST_INSERT_ID();

-- Senha temporária: TrocarSenha@2026 — TROCAR no primeiro login, via PUT /master/senha
-- ("Minha conta" na área do master).
INSERT INTO usuario (
  public_id, empresa_id, nome, login, senha, rg, cpf, data_nascimento, celular, cargo,
  endereco, numero_residencial, complemento, bairro, uf, cidade, cep,
  observacoes, url_picture, bloqueado, criado_em, atualizado_em
) VALUES (
  UUID(),
  @empresa_master_id,
  'Master JuridiqSystem',
  'master@juridiqsystem.com.br',
  '$2a$10$LVsTBTJteec5M6EI0YyJweYXOy.X62oum4p7dYqJeOGU0/Ej.Fy5a',
  '000000000',
  '00000000000',
  '2000-01-01',
  '00000000000',
  'MASTER',
  'Interno',
  '0',
  NULL,
  'Interno',
  'SP',
  'Sao Paulo',
  '00000-000',
  'Usuário master interno, gerado pela migration de bootstrap.',
  'assets/img/avatar/padrao.jpeg',
  FALSE,
  NOW(), NOW()
);

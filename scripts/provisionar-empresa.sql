-- Script de referência para provisionar uma empresa nova + seu primeiro usuário ADMINISTRADOR.
-- Não é uma migration Flyway (não roda sozinho no deploy) — é rodado manualmente, uma vez por
-- empresa nova onboardada, direto no banco.
--
-- Passo 1: gerar o hash da senha do primeiro admin (mesmo BCrypt usado pelo login, força padrão):
--   mvn compile
--   java -cp target/classes br.edu.faculdadevincit.crm_vincit.tools.GeradorHashSenha "senha-do-admin"
-- Copiar o hash impresso (começa com "$2a$10$...") e colar no lugar de <HASH_BCRYPT_AQUI> abaixo.
--
-- Passo 2: preencher os placeholders <...> com os dados reais da empresa/admin e rodar este
-- script (numa única transação, pra não deixar a empresa criada sem nenhum usuário se o segundo
-- INSERT falhar por algum motivo).

START TRANSACTION;

INSERT INTO empresa (
  codigo, nome, logo_url, timezone,
  protocolo_risco_horas, notificacao_visual_habilitada, notificacao_sonora_habilitada,
  criado_em, atualizado_em
) VALUES (
  '<codigo-curto-unico>',      -- ex.: "acme-adv" — é o que o funcionário digita no login pra identificar a empresa
  '<Nome da Empresa Ltda>',
  NULL,                         -- logo_url, pode ficar NULL por enquanto
  'America/Sao_Paulo',
  24,                            -- protocolo_risco_horas (padrão 24, igual ao que já era global antes)
  TRUE,                          -- notificacao_visual_habilitada
  TRUE,                          -- notificacao_sonora_habilitada
  NOW(), NOW()
);

SET @empresa_id = LAST_INSERT_ID();

INSERT INTO usuario (
  empresa_id, nome, login, senha, rg, cpf, data_nascimento, celular, cargo,
  endereco, numero_residencial, complemento, bairro, uf, cidade, cep,
  observacoes, url_picture, bloqueado, criado_em, atualizado_em
) VALUES (
  @empresa_id,
  '<Nome do Administrador>',
  '<admin@empresa.com>',        -- login: único dentro desta empresa (pode repetir entre empresas diferentes)
  '<HASH_BCRYPT_AQUI>',
  '<RG>',
  '<CPF>',
  '<AAAA-MM-DD>',
  '<celular>',
  'ADMINISTRADOR',
  '<endereço>',
  '<número>',
  NULL,
  '<bairro>',
  '<UF>',                        -- sigla, ex.: "SP"
  '<cidade>',
  '<cep>',
  NULL,
  'assets/img/avatar/padrao.jpeg',
  FALSE,
  NOW(), NOW()
);

-- O fluxo normal de criação de usuário (UsuarioService.save) também espelha todo Usuario como
-- um Participante (tipo FUNCIONARIO) — usado no chat interno. Reproduzido aqui pra não deixar o
-- admin provisionado manualmente faltando dos contatos do chat interno.
INSERT INTO participante (
  empresa_id, nome, login, rg, cpf, data_nascimento, celular,
  endereco, numero_residencial, complemento, bairro, uf, cidade, observacoes,
  tipo_participante, url_picture, criado_em, atualizado_em
) VALUES (
  @empresa_id,
  '<Nome do Administrador>',
  '<admin@empresa.com>',
  '<RG>',
  '<CPF>',
  '<AAAA-MM-DD>',
  '<celular>',
  '<endereço>',
  '<número>',
  NULL,
  '<bairro>',
  '<UF>',
  '<cidade>',
  NULL,
  'FUNCIONARIO',
  'assets/img/avatar/padrao.jpeg',
  NOW(), NOW()
);

-- Confirma antes de fazer commit: confira os IDs gerados e se o admin consegue logar com
-- codigoEmpresa = '<codigo-curto-unico>' antes de considerar o onboarding concluído.
COMMIT;

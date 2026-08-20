-- Etapa 3 de 5: colunas novas em usuario, ainda permissivas (cargo_id NULL, master com DEFAULT)
-- para nao quebrar as linhas existentes antes do backfill. A FK e o NOT NULL de cargo_id entram
-- so na etapa 5, depois que o backfill garantir que nenhum usuario ficou sem cargo.
--
-- master substitui o antigo valor de enum usuario.cargo = 'MASTER': super-admin da plataforma e
-- ortogonal ao cargo de empresa-cliente (o master tambem precisa de um cargo_id valido, por
-- causa da FK, mas getAuthorities() nem chega a olhar pra ele).
ALTER TABLE usuario ADD COLUMN cargo_id BIGINT NULL;
ALTER TABLE usuario ADD COLUMN master BOOLEAN NOT NULL DEFAULT FALSE;

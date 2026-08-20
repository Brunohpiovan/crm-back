-- Etapa 4 de 5: SQL puro, sem depender de nenhuma mudanca de entidade.
--
-- Dois cargos por empresa existente, espelhando exatamente o comportamento de hoje:
--   * "Administrador" (administrador = TRUE) -> quem era usuario.cargo = 'ADMINISTRADOR';
--   * "Funcionario"   (administrador = FALSE) -> quem era usuario.cargo = 'VENDEDOR'.
-- Nenhuma linha em cargo_permissao de proposito: VENDEDOR nao tinha nenhuma das permissoes
-- novas, entao o cargo comum nasce sem permissao nenhuma (o administrador da empresa marca os
-- checkboxes que quiser depois, na tela "Cargos e Permissoes").
--
-- UUID() e avaliado por linha no MySQL, entao cada cargo recebe seu proprio public_id.
INSERT INTO cargo (empresa_id, public_id, nome, administrador, criado_em, atualizado_em)
SELECT e.id, UUID(), 'Administrador', TRUE, NOW(), NOW() FROM empresa e;

INSERT INTO cargo (empresa_id, public_id, nome, administrador, criado_em, atualizado_em)
SELECT e.id, UUID(), 'Funcionário', FALSE, NOW(), NOW() FROM empresa e;

UPDATE usuario u
JOIN cargo c ON c.empresa_id = u.empresa_id AND c.administrador = TRUE
SET u.cargo_id = c.id
WHERE u.cargo = 'ADMINISTRADOR';

UPDATE usuario u
JOIN cargo c ON c.empresa_id = u.empresa_id AND c.administrador = FALSE
SET u.cargo_id = c.id
WHERE u.cargo = 'VENDEDOR';

UPDATE usuario SET master = TRUE WHERE cargo = 'MASTER';

-- O master tambem precisa de um cargo_id valido (NOT NULL + FK na etapa 5). Recebe o cargo
-- administrador da propria empresa interna: irrelevante para o acesso real, ja que
-- getAuthorities() curto-circuita em master = TRUE antes de olhar o cargo.
UPDATE usuario u
JOIN cargo c ON c.empresa_id = u.empresa_id AND c.administrador = TRUE
SET u.cargo_id = c.id
WHERE u.cargo = 'MASTER';

-- Habilita invalidacao de sessao: logout e troca de senha incrementam sessao_versao, e o
-- SecurityFilter passa a rejeitar qualquer JWT emitido com uma versao antiga (claim
-- sessionVersion != usuario.sessaoVersao). Sem isso, um token continuava valido ate a expiracao
-- natural (12h) mesmo depois de logout ou troca de senha.

ALTER TABLE usuario ADD COLUMN sessao_versao INT NOT NULL DEFAULT 0;

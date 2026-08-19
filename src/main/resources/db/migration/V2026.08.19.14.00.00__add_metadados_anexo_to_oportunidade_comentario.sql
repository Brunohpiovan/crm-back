-- Anexo de comentario deixa de ser so imagem: escritorio de advocacia troca peticao, procuracao e
-- planilha no proprio card da oportunidade. Para isso o anexo precisa de identidade propria.
--
-- nome_anexo guarda o nome original que o usuario ve e baixa ("contrato-assinado.pdf"). Ele NAO e
-- usado para montar a key no S3: a key continua sendo um UUID mais a extensao derivada do tipo
-- confirmado pelo servidor, entao nome de arquivo malicioso nao vira caminho nem extensao.
--
-- tipo_anexo guarda o content-type ja validado por assinatura de bytes (ver
-- AnexoComentarioValidator), nao o que o cliente declarou. E o que decide se o arquivo abre no
-- navegador ou desce como download.
ALTER TABLE oportunidade_comentario ADD COLUMN nome_anexo VARCHAR(255) NULL;
ALTER TABLE oportunidade_comentario ADD COLUMN tipo_anexo VARCHAR(120) NULL;
ALTER TABLE oportunidade_comentario ADD COLUMN tamanho_anexo BIGINT NULL;

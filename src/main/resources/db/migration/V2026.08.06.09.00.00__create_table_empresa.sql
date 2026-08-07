-- Base do multi-tenant (multi-empresa): toda entidade de negócio passa a pertencer a uma
-- Empresa. Esta migration só cria a tabela e a linha "Empresa Padrão" (id = 1), pra onde todo
-- dado que já existe hoje vai apontar nas próximas duas migrations (add_empresa_id_to_tenant_tables
-- e add_fk_e_unique_empresa). Isolamento de fato é feito no Hibernate via @TenantId
-- (TenantContext/TenantIdentifierResolver), não aqui — esta migration é só o schema.
CREATE TABLE empresa (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    logo_url VARCHAR(500),
    timezone VARCHAR(60) NOT NULL,
    protocolo_risco_horas INT NOT NULL,
    notificacao_visual_habilitada BOOLEAN NOT NULL,
    notificacao_sonora_habilitada BOOLEAN NOT NULL,
    criado_em DATETIME,
    atualizado_em DATETIME,
    CONSTRAINT uk_empresa_codigo UNIQUE (codigo)
);


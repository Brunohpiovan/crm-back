-- Cargo customizavel por empresa ("Advogado", "Secretario", "Estagiario"...), substituindo o
-- enum fixo usuario.cargo (ADMINISTRADOR/VENDEDOR/MASTER). Cada empresa tem exatamente um cargo
-- com administrador = TRUE: acesso total, criado junto com a empresa (ver o backfill deste
-- mesmo lote e EmpresaService.create), nao excluivel e nao editavel pela API.
--
-- Etapa 1 de 5 (criacao -> backfill -> not-null/drop), seguindo o padrao ja usado em
-- V2026.08.05.20.00.00 e V2026.08.07.10.00.00.
CREATE TABLE cargo (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL,
    public_id VARCHAR(36) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    administrador BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em DATETIME NULL,
    atualizado_em DATETIME NULL,
    CONSTRAINT uk_cargo_public_id UNIQUE (public_id),
    CONSTRAINT uk_cargo_empresa_nome UNIQUE (empresa_id, nome),
    CONSTRAINT fk_cargo_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id)
);

-- Suporta a listagem da tela de cargos e a busca do cargo administrador da empresa
-- (CargoRepository.findByEmpresaIdAndAdministradorTrue).
CREATE INDEX idx_cargo_empresa_administrador ON cargo (empresa_id, administrador);

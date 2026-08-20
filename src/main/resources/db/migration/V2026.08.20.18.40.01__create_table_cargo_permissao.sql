-- Permissoes delegadas a um cargo (@ElementCollection de Cargo.permissoes). O catalogo de
-- valores validos e o enum Permissao (fixo do sistema, nao editavel pela empresa) --
-- deliberadamente sem CHECK constraint aqui: o enum cresce a cada nova area delegavel, e um
-- CHECK exigiria uma migration extra so pra reescrever a lista a cada valor novo.
--
-- Um cargo com administrador = TRUE nao depende desta tabela: getAuthorities() concede todas as
-- permissoes pelo flag, para nunca existir "admin capado" por linha faltando aqui.
CREATE TABLE cargo_permissao (
    cargo_id BIGINT NOT NULL,
    permissao VARCHAR(60) NOT NULL,
    PRIMARY KEY (cargo_id, permissao),
    CONSTRAINT fk_cargo_permissao_cargo FOREIGN KEY (cargo_id) REFERENCES cargo(id)
);

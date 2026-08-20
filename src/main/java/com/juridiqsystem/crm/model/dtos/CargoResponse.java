package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Cargo;
import com.juridiqsystem.crm.model.enums.Permissao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Cargo como devolvido por GET /cargos. `permissoes` traz só o que está gravado em
 * cargo_permissao; para o cargo administrador a lista vem vazia de propósito — quem tem
 * `administrador = true` tem acesso total independente dela (ver Usuario.getAuthorities()),
 * e o frontend deve tratar esse caso como "tudo liberado", não listar permissão por permissão.
 */
@Schema(description = "Cargo da empresa, com as permissões delegadas a ele.")
public record CargoResponse(
        @Schema(description = "Identificador público (UUID) do cargo") String id,
        @Schema(description = "Nome livre definido pela empresa (ex.: \"Advogado\")") String nome,
        @Schema(description = "Se true, é o cargo fixo de administrador: acesso total, não editável nem excluível") boolean administrador,
        @Schema(description = "Permissões delegadas (vazio quando administrador = true)") List<Permissao> permissoes
) {
    public static CargoResponse from(Cargo cargo) {
        return new CargoResponse(
                cargo.getPublicId(),
                cargo.getNome(),
                cargo.isAdministrador(),
                cargo.isAdministrador() ? List.of() : List.copyOf(cargo.getPermissoes())
        );
    }
}

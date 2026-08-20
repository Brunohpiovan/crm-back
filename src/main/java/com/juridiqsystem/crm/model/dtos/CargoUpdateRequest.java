package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.enums.Permissao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload de atualização de cargo (PUT /cargos/{publicId}, restrito a ROLE_ADMIN). Só se aplica
 * a cargos customizados: o cargo administrador da empresa é rejeitado pelo service (é sempre
 * "acesso total", não faz sentido editar sua lista de permissões).
 */
@Schema(description = "Payload de atualização de um cargo customizado da empresa.")
public record CargoUpdateRequest(
        @NotBlank(message = "Informe o nome do cargo")
        @Size(max = 100, message = "O nome do cargo deve ter no máximo 100 caracteres")
        @Schema(description = "Novo nome do cargo, único dentro da empresa", example = "Advogado Sênior") String nome,

        @Schema(description = "Conjunto completo de permissões do cargo (substitui o anterior); vazio remove todas")
        List<Permissao> permissoes
) {
}

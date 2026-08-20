package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.enums.Permissao;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Item do catálogo estático de permissões (GET /cargos/permissoes-disponiveis). Existe para o
 * frontend renderizar os checkboxes sem duplicar a lista de Permissao nem os rótulos.
 */
@Schema(description = "Permissão delegável do sistema, com rótulo amigável para exibição.")
public record PermissaoResponse(
        @Schema(description = "Chave técnica da permissão (valor do enum Permissao)", example = "GERENCIAR_FUNIL") String chave,
        @Schema(description = "Rótulo amigável para exibir ao administrador", example = "Gerenciar funis e etapas") String rotulo
) {
    public static PermissaoResponse from(Permissao permissao) {
        return new PermissaoResponse(permissao.name(), permissao.getRotulo());
    }
}

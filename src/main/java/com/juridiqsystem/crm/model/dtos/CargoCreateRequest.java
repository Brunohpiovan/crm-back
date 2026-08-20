package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.enums.Permissao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload de criação de cargo (POST /cargos, restrito a ROLE_ADMIN). Não há campo
 * `administrador`: um cargo criado pela API nasce sempre como não-administrador — o cargo de
 * administrador é único por empresa e criado junto com a própria empresa.
 */
@Schema(description = "Payload de criação de um cargo customizado da empresa.")
public record CargoCreateRequest(
        @NotBlank(message = "Informe o nome do cargo")
        @Size(max = 100, message = "O nome do cargo deve ter no máximo 100 caracteres")
        @Schema(description = "Nome do cargo, único dentro da empresa", example = "Advogado") String nome,

        @Schema(description = "Permissões delegadas ao cargo; omitido/vazio cria um cargo sem nenhuma permissão extra")
        List<Permissao> permissoes
) {
}

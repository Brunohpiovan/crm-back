package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Dados atualizados de uma oportunidade, enviados como a parte JSON `oportunidade` de uma requisição multipart/form-data para PUT /oportunidade/{id}.")
public record OportunidadeUpdateRequest(
        @NotBlank(message = "Informe um titulo") @Size(max = 150, message = "O titulo deve ter no maximo 150 caracteres") String titulo,
        @Schema(description = "Id da etapa em que a oportunidade deve ficar. Se diferente da etapa atual, os valores totais das etapas envolvidas são recalculados.") @NotNull(message = "Informe a etapa") String etapaId,
        @Schema(description = "Id do usuário responsável (vendedor/dono) pela oportunidade") @NotNull(message = "Informe o criador") String criadorId,
        @Schema(description = "Dados atualizados do cliente/participante associado à oportunidade") @NotNull(message = "Informe o cliente") @Valid OportunidadeClienteRequest cliente,
        @NotNull(message = "Informe o valor") BigDecimal valor,
        LocalDateTime dataCriacao,
        @Schema(description = "Origem/canal pelo qual a oportunidade chegou") Origem origem,
        String interesse,
        @Schema(description = "Detalhamento adicional; só é persistido quando origem = OUTRO, sendo descartado nos demais casos") String descricao,
        String observacoes,
        @Schema(description = "Situação atual da oportunidade") SituacaoOportunidade situacao,
        @Schema(description = "Ids das tags a associar à oportunidade; todos precisam existir, senão a requisição falha") List<String> tagIds,
        @Schema(description = "URL do anexo atual a ser mantido. Envie null para remover o anexo existente (quando nenhum novo arquivo for enviado na parte `file`); se um novo `file` for enviado, este valor é ignorado e o anexo é substituído.") String urlAnexo
) {
}

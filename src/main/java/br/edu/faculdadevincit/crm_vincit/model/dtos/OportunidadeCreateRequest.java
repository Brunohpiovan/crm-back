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

@Schema(description = "Dados de uma nova oportunidade, enviados como a parte JSON `oportunidade` de uma requisição multipart/form-data para POST /oportunidade.")
public record OportunidadeCreateRequest(
        @NotBlank(message = "Informe um titulo") @Size(max = 150, message = "O titulo deve ter no maximo 150 caracteres") String titulo,
        @Schema(description = "Id da etapa em que a oportunidade será criada") @NotNull(message = "Informe a etapa") String etapaId,
        @Schema(description = "Id do usuário responsável (vendedor/dono) pela oportunidade") @NotNull(message = "Informe o criador") String criadorId,
        @Schema(description = "Dados do cliente/participante associado à oportunidade. Se já existir um participante com o mesmo celular, ele é reaproveitado; senão, um novo é criado.") @NotNull(message = "Informe o cliente") @Valid OportunidadeClienteRequest cliente,
        @NotNull(message = "Informe o valor") BigDecimal valor,
        @Schema(description = "Data/hora de criação da oportunidade; se omitida, é preenchida automaticamente com o momento da requisição") LocalDateTime dataCriacao,
        @Schema(description = "Origem/canal pelo qual a oportunidade chegou") Origem origem,
        String interesse,
        @Schema(description = "Detalhamento adicional, tipicamente preenchido quando origem = OUTRO") String descricao,
        String observacoes,
        @Schema(description = "Situação atual da oportunidade; se omitida, é definida como ABERTO") SituacaoOportunidade situacao,
        @Schema(description = "Ids das tags a associar à oportunidade; todos precisam existir, senão a requisição falha") List<String> tagIds
) {
}

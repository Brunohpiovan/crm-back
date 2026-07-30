package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OportunidadeCreateRequest(
        @NotBlank(message = "Informe um titulo") @Size(max = 150, message = "O titulo deve ter no maximo 150 caracteres") String titulo,
        @NotNull(message = "Informe a etapa") Long etapaId,
        @NotNull(message = "Informe o criador") Long criadorId,
        @NotNull(message = "Informe o cliente") @Valid OportunidadeClienteRequest cliente,
        @NotNull(message = "Informe o valor") BigDecimal valor,
        LocalDateTime dataCriacao,
        Origem origem,
        @Size(max = 100, message = "O campo interesse deve ter no maximo 100 caracteres") String interesse,
        @Size(max = 150, message = "A descricao deve ter no maximo 150 caracteres") String descricao,
        @Size(max = 255, message = "A observacao deve ter no maximo 255 caracteres") String observacoes,
        SituacaoOportunidade situacao,
        List<Long> tagIds
) {
}

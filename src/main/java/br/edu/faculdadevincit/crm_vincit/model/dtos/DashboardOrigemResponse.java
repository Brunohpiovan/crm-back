package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Quantidade de oportunidades agrupadas por origem/canal no período filtrado.")
public record DashboardOrigemResponse(
        @Schema(description = "Origem/canal da oportunidade") Origem origem,
        @Schema(description = "Quantidade de oportunidades com esta origem") Long quantidade,
        @Schema(description = "Percentual em relação ao total de oportunidades retornadas neste bloco") Double percentual
) {
}

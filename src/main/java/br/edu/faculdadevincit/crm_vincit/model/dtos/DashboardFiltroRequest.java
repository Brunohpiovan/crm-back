package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardFiltroRequest {

    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final Long pipelineId;
    private final Long userId;
    private final SituacaoOportunidade status;
    private final Origem origin;
    private final List<Long> tagIds;
    private final List<Long> funilIdsPermitidos;
}

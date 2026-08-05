package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardResponse;
import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import br.edu.faculdadevincit.crm_vincit.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Dashboard", description = """
        Analytics/agregação para a tela de dashboard: uma única requisição carrega todos os \
        blocos (resumo, funil de vendas, origem de leads, série diária de protocolos, ranking \
        de usuários e panorama de cadências). Todos os componentes respeitam os mesmos filtros. \
        Resultado cacheado em memória por 30s por combinação de filtros + usuário autenticado.
        """)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Operation(summary = "Carregar todos os indicadores do dashboard",
            description = """
                    Requer JWT. Se nenhum filtro de período for informado, usa os últimos 30 dias. \
                    Escopo de visibilidade: um usuário com cargo VENDEDOR só vê oportunidades/funil/origem/cadências \
                    dos pipelines em que está vinculado, e protocolos/ranking apenas dos seus próprios atendimentos \
                    (informar userId de outro usuário retorna 403 nesse caso). O parâmetro teamId é aceito por \
                    compatibilidade com o frontend mas hoje não tem efeito: não existe uma entidade de equipe/time \
                    no schema atual, apenas o vínculo usuário-pipeline (Funil.funcionarios).
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores do dashboard",
                    content = @Content(schema = @Schema(implementation = DashboardResponse.class))),
            @ApiResponse(responseCode = "403", description = "userId informado é de outro usuário e o autenticado não é ADMINISTRADOR, ou pipelineId informado não é acessível ao usuário autenticado")
    })
    @GetMapping
    public DashboardResponse getDashboard(
            @Parameter(description = "Início do período (ISO-8601, ex.: 2026-07-01T00:00:00). Padrão: 30 dias atrás.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fim do período (ISO-8601). Padrão: agora.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Id do funil/pipeline para restringir o resultado")
            @RequestParam(required = false) Long pipelineId,
            @Parameter(description = "Id do usuário para restringir o resultado (obrigatoriamente o próprio id para quem não é ADMINISTRADOR)")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "Não possui dimensão correspondente no schema atual; aceito e ignorado")
            @RequestParam(required = false) Long teamId,
            @Parameter(description = "Situação da oportunidade (ABERTO, GANHO, PERDIDO, CONGELADO, LIXEIRA)")
            @RequestParam(required = false) SituacaoOportunidade status,
            @Parameter(description = "Origem/canal da oportunidade")
            @RequestParam(required = false) Origem origin,
            @Parameter(description = "Ids de tags para restringir o resultado a oportunidades com pelo menos uma delas")
            @RequestParam(required = false) List<Long> tags
    ) {
        return dashboardService.getDashboard(startDate, endDate, pipelineId, userId, status, origin, tags);
    }
}

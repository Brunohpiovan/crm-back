package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.DashboardRankingResponse;
import com.juridiqsystem.crm.model.dtos.DashboardResponse;
import com.juridiqsystem.crm.model.dtos.PageResponse;
import com.juridiqsystem.crm.model.enums.Origem;
import com.juridiqsystem.crm.model.enums.SituacaoOportunidade;
import com.juridiqsystem.crm.service.DashboardService;
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
                    (informar userId de outro usuário retorna 403 nesse caso). O parâmetro teamId filtra pelos \
                    membros da equipe informada (ver /equipe): para ADMINISTRADOR amplia o resultado para o time \
                    inteiro; para quem não é ADMINISTRADOR só é aceito se o autenticado for membro da equipe \
                    (senão 403), mas o resultado continua restrito ao próprio usuário — Equipe não modela \
                    líder/permissão elevada, então não dá visão sobre colegas.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores do dashboard",
                    content = @Content(schema = @Schema(implementation = DashboardResponse.class))),
            @ApiResponse(responseCode = "403", description = "userId de outro usuário, pipelineId fora do escopo, ou teamId de uma equipe da qual o autenticado não é membro (quando não é ADMINISTRADOR)")
    })
    @GetMapping
    public DashboardResponse getDashboard(
            @Parameter(description = "Início do período (ISO-8601, ex.: 2026-07-01T00:00:00). Padrão: 30 dias atrás.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fim do período (ISO-8601). Padrão: agora.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Id do funil/pipeline para restringir o resultado")
            @RequestParam(required = false) String pipelineId,
            @Parameter(description = "Id do usuário para restringir o resultado (obrigatoriamente o próprio id para quem não é ADMINISTRADOR)")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Id da equipe (ver /equipe) para restringir o resultado aos seus membros")
            @RequestParam(required = false) String teamId,
            @Parameter(description = "Situação(ões) da oportunidade (ABERTO, GANHO, PERDIDO, CONGELADO, LIXEIRA). Repita o parâmetro para filtrar por mais de uma (ex.: status=ABERTO&status=CONGELADO).")
            @RequestParam(required = false) List<SituacaoOportunidade> status,
            @Parameter(description = "Origem(ns)/canal da oportunidade. Repita o parâmetro para filtrar por mais de uma.")
            @RequestParam(required = false) List<Origem> origin,
            @Parameter(description = "Ids de tags para restringir o resultado a oportunidades com pelo menos uma delas")
            @RequestParam(required = false) List<String> tags
    ) {
        return dashboardService.getDashboard(startDate, endDate, pipelineId, userId, teamId, status, origin, tags);
    }

    @Operation(summary = "Ranking paginado de usuários",
            description = """
                    Requer JWT. Mesmos filtros e regras de escopo de GET /dashboard, mas devolve o ranking \
                    completo com paginação (o bloco "ranking" de GET /dashboard não é paginado, é sempre a \
                    lista inteira — este endpoint existe para quando essa lista for grande, ex.: "ver ranking \
                    completo" no front). Ordenado por valorVendido decrescente.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página do ranking",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "403", description = "userId de outro usuário, pipelineId fora do escopo, ou teamId de uma equipe da qual o autenticado não é membro (quando não é ADMINISTRADOR)")
    })
    @GetMapping("/ranking")
    public PageResponse<DashboardRankingResponse> getRanking(
            @Parameter(description = "Início do período (ISO-8601). Padrão: 30 dias atrás.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Fim do período (ISO-8601). Padrão: agora.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Id do funil/pipeline para restringir o resultado")
            @RequestParam(required = false) String pipelineId,
            @Parameter(description = "Id do usuário para restringir o resultado (obrigatoriamente o próprio id para quem não é ADMINISTRADOR)")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Id da equipe (ver /equipe) para restringir o resultado aos seus membros")
            @RequestParam(required = false) String teamId,
            @Parameter(description = "Situação(ões) da oportunidade. Repita o parâmetro para mais de uma.")
            @RequestParam(required = false) List<SituacaoOportunidade> status,
            @Parameter(description = "Origem(ns)/canal da oportunidade. Repita o parâmetro para mais de uma.")
            @RequestParam(required = false) List<Origem> origin,
            @Parameter(description = "Ids de tags para restringir o resultado")
            @RequestParam(required = false) List<String> tags,
            @Parameter(description = "Página, base 0. Padrão: 0.")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página. Padrão: 20, máximo 100.")
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return dashboardService.getRanking(startDate, endDate, pipelineId, userId, teamId, status, origin, tags, page, size);
    }
}

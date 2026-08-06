package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.config.CacheConfig;
import br.edu.faculdadevincit.crm_vincit.model.CadenciaFunil;
import br.edu.faculdadevincit.crm_vincit.model.Funil;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardCadenciaResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardFiltroRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardFunilEtapaResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardOrigemResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardRankingOportunidadeRow;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardRankingProtocoloRow;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardRankingResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardSerieDiariaResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.DashboardSummaryResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.PageResponse;
import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import br.edu.faculdadevincit.crm_vincit.repository.CadenciaFunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.EquipeRepository;
import br.edu.faculdadevincit.crm_vincit.repository.FunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.LogMovimentacaoCadenciaRepository;
import br.edu.faculdadevincit.crm_vincit.repository.OportunidadeRepository;
import br.edu.faculdadevincit.crm_vincit.repository.OportunidadeRepository.DiaValorProjection;
import br.edu.faculdadevincit.crm_vincit.repository.ProtocoloRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ProtocoloRepository.DiaContagemProjection;
import br.edu.faculdadevincit.crm_vincit.repository.ProtocoloRepository.DiaTempoMedioProjection;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final int PERIODO_PADRAO_DIAS = 30;
    private static final int RANKING_PAGE_SIZE_MAXIMO = 100;
    private static final List<Long> USUARIO_IDS_DUMMY_NATIVE_QUERY = List.of(-1L);
    private static final List<String> ORIGEM_DUMMY_NATIVE_QUERY = List.of("__NENHUMA__");

    // horarioMovimentacao (CadenciaFunil) é configurado pensando em horário de Brasília - ver
    // mesma constante em CadenciaFunilService, que dispara a movimentação de fato.
    private static final ZoneId FUSO_HORARIO_MOVIMENTACAO = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private ProtocoloRepository protocoloRepository;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private CadenciaFunilRepository cadenciaFunilRepository;

    @Autowired
    private FunilRepository funilRepository;

    @Autowired
    private EquipeRepository equipeRepository;

    @Autowired
    private LogMovimentacaoCadenciaRepository logMovimentacaoCadenciaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${dashboard.protocolo.risco-horas:24}")
    private long protocoloRiscoHoras;

    @Cacheable(value = CacheConfig.DASHBOARD_CACHE,
            key = "T(org.springframework.security.core.context.SecurityContextHolder).context.authentication.name" +
                    " + '|' + #startDateParam + '|' + #endDateParam + '|' + #pipelineId + '|' + #userIdFiltro" +
                    " + '|' + #teamId + '|' + #status + '|' + #origin + '|' + #tagIds")
    public DashboardResponse getDashboard(LocalDateTime startDateParam, LocalDateTime endDateParam, Long pipelineId,
                                           Long userIdFiltro, Long teamId, List<SituacaoOportunidade> status,
                                           List<Origem> origin, List<Long> tagIds) {
        Usuario usuario = getUsuarioAutenticado();

        LocalDateTime endDate = endDateParam != null ? endDateParam : LocalDateTime.now();
        LocalDateTime startDate = startDateParam != null ? startDateParam : endDate.minusDays(PERIODO_PADRAO_DIAS);

        List<Long> userIds = resolverUserIdsComAutorizacao(usuario, userIdFiltro, teamId);
        List<Long> funilIdsPermitidos = resolverFunilIdsPermitidos(usuario, pipelineId);
        if (funilIdsPermitidos.isEmpty()) {
            return dashboardVazio();
        }

        List<SituacaoOportunidade> statusNormalizado = status != null ? status : List.of();
        List<Origem> originNormalizado = origin != null ? origin : List.of();
        List<Long> tagsNormalizadas = tagIds != null ? tagIds : List.of();
        DashboardFiltroRequest filtro = new DashboardFiltroRequest(
                startDate, endDate, pipelineId, userIds, statusNormalizado, originNormalizado, tagsNormalizadas, funilIdsPermitidos);

        return new DashboardResponse(
                montarSummary(filtro),
                montarFunil(filtro),
                montarOrigens(filtro),
                montarSerieDiaria(filtro),
                montarRanking(filtro),
                montarCadencias(filtro));
    }

    /**
     * Ranking paginado — endpoint próprio (GET /dashboard/ranking), separado de getDashboard.
     * O merge de montarRanking já é feito em memória (ver comentário do próprio método: duas
     * queries pequenas, uma linha por usuário com atividade no período — nunca milhares de
     * linhas), então paginar com subList depois do merge é o trade-off certo aqui; paginação
     * real no banco exigiria reescrever como uma única query com UNION (MySQL não tem FULL
     * OUTER JOIN), o que não compensa pelo volume real.
     */
    @Cacheable(value = CacheConfig.DASHBOARD_CACHE,
            key = "T(org.springframework.security.core.context.SecurityContextHolder).context.authentication.name" +
                    " + '|ranking|' + #startDateParam + '|' + #endDateParam + '|' + #pipelineId + '|' + #userIdFiltro" +
                    " + '|' + #teamId + '|' + #status + '|' + #origin + '|' + #tagIds + '|' + #page + '|' + #size")
    public PageResponse<DashboardRankingResponse> getRanking(LocalDateTime startDateParam, LocalDateTime endDateParam, Long pipelineId,
                                                               Long userIdFiltro, Long teamId, List<SituacaoOportunidade> status,
                                                               List<Origem> origin, List<Long> tagIds, int page, int size) {
        Usuario usuario = getUsuarioAutenticado();

        LocalDateTime endDate = endDateParam != null ? endDateParam : LocalDateTime.now();
        LocalDateTime startDate = startDateParam != null ? startDateParam : endDate.minusDays(PERIODO_PADRAO_DIAS);

        List<Long> userIds = resolverUserIdsComAutorizacao(usuario, userIdFiltro, teamId);
        List<Long> funilIdsPermitidos = resolverFunilIdsPermitidos(usuario, pipelineId);

        int tamanhoValido = Math.max(1, Math.min(size, RANKING_PAGE_SIZE_MAXIMO));
        int paginaValida = Math.max(0, page);

        if (funilIdsPermitidos.isEmpty()) {
            return PageResponse.of(List.of(), paginaValida, tamanhoValido);
        }

        List<SituacaoOportunidade> statusNormalizado = status != null ? status : List.of();
        List<Origem> originNormalizado = origin != null ? origin : List.of();
        List<Long> tagsNormalizadas = tagIds != null ? tagIds : List.of();
        DashboardFiltroRequest filtro = new DashboardFiltroRequest(
                startDate, endDate, pipelineId, userIds, statusNormalizado, originNormalizado, tagsNormalizadas, funilIdsPermitidos);

        return PageResponse.of(montarRanking(filtro), paginaValida, tamanhoValido);
    }

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        return (Usuario) usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    /**
     * Resolve userId + teamId para uma única lista de ids elegível para os filtros do dashboard.
     * Lista vazia = sem restrição (só é possível para ADMINISTRADOR sem nenhum dos dois filtros).
     * teamId só amplia a visão de ADMINISTRADOR (vê o time inteiro); para quem não é
     * ADMINISTRADOR, o resultado continua restrito ao próprio id mesmo informando uma equipe da
     * qual é membro — só serve para não dar erro, não para ver dados de colegas (a entidade
     * Equipe não tem noção de líder/permissão elevada).
     */
    private List<Long> resolverUserIdsComAutorizacao(Usuario usuario, Long userIdFiltro, Long teamId) {
        boolean administrador = usuario.getCargo() == UserRole.ADMINISTRADOR;
        List<Long> membrosDaEquipe = null;

        if (teamId != null) {
            if (!equipeRepository.existsById(teamId)) {
                throw new AccessDeniedException("Você não tem acesso a esta equipe.");
            }
            if (!administrador && !equipeRepository.existsByIdAndMembrosContains(teamId, usuario)) {
                throw new AccessDeniedException("Você não tem acesso a esta equipe.");
            }
            membrosDaEquipe = equipeRepository.findMembroIdsById(teamId);
        }

        if (administrador) {
            if (userIdFiltro == null) {
                return membrosDaEquipe != null ? membrosDaEquipe : List.of();
            }
            if (membrosDaEquipe != null && !membrosDaEquipe.contains(userIdFiltro)) {
                throw new AccessDeniedException("O usuário informado não pertence à equipe informada.");
            }
            return List.of(userIdFiltro);
        }

        if (userIdFiltro != null && !userIdFiltro.equals(usuario.getId())) {
            throw new AccessDeniedException("Você só pode consultar seus próprios indicadores.");
        }
        return List.of(usuario.getId());
    }

    private List<Long> resolverFunilIdsPermitidos(Usuario usuario, Long pipelineId) {
        List<Long> permitidos = usuario.getCargo() == UserRole.ADMINISTRADOR
                ? funilRepository.findAll().stream().map(Funil::getId).toList()
                : funilRepository.findByFuncionariosContains(usuario).stream().map(Funil::getId).toList();

        if (pipelineId != null && !permitidos.contains(pipelineId)) {
            throw new AccessDeniedException("Você não tem acesso a este pipeline.");
        }
        return permitidos;
    }

    private DashboardSummaryResponse montarSummary(DashboardFiltroRequest filtro) {
        DashboardFiltroRequest filtroAnterior = filtroPeriodoAnterior(filtro);

        BigDecimal valorAtual = oportunidadeRepository.sumValorPorSituacao(filtro, SituacaoOportunidade.ABERTO);
        BigDecimal valorAnterior = oportunidadeRepository.sumValorPorSituacao(filtroAnterior, SituacaoOportunidade.ABERTO);
        Double variacaoPercentual = calcularVariacaoPercentual(valorAtual, valorAnterior);

        long oportunidadesAbertas = oportunidadeRepository.countPorSituacao(filtro, SituacaoOportunidade.ABERTO);
        long oportunidadesGanhas = oportunidadeRepository.countPorSituacao(filtro, SituacaoOportunidade.GANHO);
        long oportunidadesPerdidas = oportunidadeRepository.countPorSituacao(filtro, SituacaoOportunidade.PERDIDO);
        Double taxaConversao = calcularTaxaConversao(oportunidadesGanhas, oportunidadesPerdidas);

        long protocolosAbertos = protocoloRepository.countAbertos(filtro);
        LocalDateTime limiteRisco = LocalDateTime.now().minusHours(protocoloRiscoHoras);
        long protocolosEmRisco = protocoloRepository.countEmRisco(filtro, limiteRisco);
        Double tempoMedioAtendimentoMinutos = protocoloRepository.avgTempoAtendimentoMinutos(filtro);
        Double tempoMedioAtendimentoAnteriorMinutos = protocoloRepository.avgTempoAtendimentoMinutos(filtroAnterior);

        return new DashboardSummaryResponse(valorAtual, variacaoPercentual, taxaConversao, protocolosAbertos,
                protocolosEmRisco, tempoMedioAtendimentoMinutos, tempoMedioAtendimentoAnteriorMinutos,
                oportunidadesAbertas, oportunidadesGanhas, oportunidadesPerdidas);
    }

    private DashboardFiltroRequest filtroPeriodoAnterior(DashboardFiltroRequest filtro) {
        Duration duracao = Duration.between(filtro.getStartDate(), filtro.getEndDate());
        LocalDateTime endAnterior = filtro.getStartDate();
        LocalDateTime startAnterior = endAnterior.minus(duracao);
        return new DashboardFiltroRequest(startAnterior, endAnterior, filtro.getPipelineId(), filtro.getUserIds(),
                filtro.getStatus(), filtro.getOrigin(), filtro.getTagIds(), filtro.getFunilIdsPermitidos());
    }

    private Double calcularVariacaoPercentual(BigDecimal atual, BigDecimal anterior) {
        if (anterior == null || anterior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return atual.subtract(anterior)
                .divide(anterior, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private Double calcularTaxaConversao(long ganhas, long perdidas) {
        long total = ganhas + perdidas;
        if (total == 0) {
            return 0.0;
        }
        return (ganhas * 100.0) / total;
    }

    private List<DashboardFunilEtapaResponse> montarFunil(DashboardFiltroRequest filtro) {
        List<DashboardFunilEtapaResponse> etapas = oportunidadeRepository.funilPorEtapa(filtro);
        BigDecimal valorTotalFunil = etapas.stream()
                .map(DashboardFunilEtapaResponse::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DashboardFunilEtapaResponse> resultado = new ArrayList<>(etapas.size());
        int ordem = 1;
        for (DashboardFunilEtapaResponse etapa : etapas) {
            double percentual = valorTotalFunil.compareTo(BigDecimal.ZERO) == 0
                    ? 0.0
                    : etapa.valorTotal().multiply(BigDecimal.valueOf(100)).divide(valorTotalFunil, 4, RoundingMode.HALF_UP).doubleValue();
            resultado.add(new DashboardFunilEtapaResponse(etapa.etapaId(), etapa.nome(), ordem++, etapa.quantidade(), etapa.valorTotal(), percentual));
        }
        return resultado;
    }

    private List<DashboardOrigemResponse> montarOrigens(DashboardFiltroRequest filtro) {
        List<DashboardOrigemResponse> origens = oportunidadeRepository.leadsPorOrigem(filtro);
        long total = origens.stream().mapToLong(DashboardOrigemResponse::quantidade).sum();
        if (total == 0) {
            return origens;
        }
        return origens.stream()
                .map(o -> new DashboardOrigemResponse(o.origem(), o.quantidade(), (o.quantidade() * 100.0) / total))
                .toList();
    }

    private List<DashboardSerieDiariaResponse> montarSerieDiaria(DashboardFiltroRequest filtro) {
        boolean semRestricaoUsuario = filtro.getUserIds().isEmpty();
        List<Long> usuarioIdsParaQueryNativa = semRestricaoUsuario ? USUARIO_IDS_DUMMY_NATIVE_QUERY : filtro.getUserIds();

        boolean semRestricaoOrigem = filtro.getOrigin().isEmpty();
        List<String> origensParaQueryNativa = semRestricaoOrigem
                ? ORIGEM_DUMMY_NATIVE_QUERY
                : filtro.getOrigin().stream().map(Enum::name).toList();

        boolean semRestricaoTag = filtro.getTagIds().isEmpty();
        List<Long> tagIdsParaQueryNativa = semRestricaoTag ? USUARIO_IDS_DUMMY_NATIVE_QUERY : filtro.getTagIds();

        Map<LocalDate, Long> abertosPorDia = mapaPorDia(
                protocoloRepository.countAbertosPorDia(filtro.getStartDate(), filtro.getEndDate(), semRestricaoUsuario, usuarioIdsParaQueryNativa));
        Map<LocalDate, Long> fechadosPorDia = mapaPorDia(
                protocoloRepository.countFechadosPorDia(filtro.getStartDate(), filtro.getEndDate(), semRestricaoUsuario, usuarioIdsParaQueryNativa));
        Map<LocalDate, Double> tempoMedioPorDia = protocoloRepository
                .avgTempoAtendimentoPorDia(filtro.getStartDate(), filtro.getEndDate(), semRestricaoUsuario, usuarioIdsParaQueryNativa)
                .stream()
                .collect(Collectors.toMap(DiaTempoMedioProjection::getDia, DiaTempoMedioProjection::getTempoMedioMinutos));
        Map<LocalDate, BigDecimal> valorPorDia = oportunidadeRepository
                .sumValorAbertoPorDia(filtro.getStartDate(), filtro.getEndDate(), filtro.getFunilIdsPermitidos(), filtro.getPipelineId(),
                        semRestricaoUsuario, usuarioIdsParaQueryNativa, semRestricaoOrigem, origensParaQueryNativa,
                        semRestricaoTag, tagIdsParaQueryNativa)
                .stream()
                .collect(Collectors.toMap(DiaValorProjection::getDia, DiaValorProjection::getValor));

        List<DashboardSerieDiariaResponse> serie = new ArrayList<>();
        LocalDate dia = filtro.getStartDate().toLocalDate();
        LocalDate ultimoDia = filtro.getEndDate().toLocalDate();
        while (!dia.isAfter(ultimoDia)) {
            serie.add(new DashboardSerieDiariaResponse(dia, abertosPorDia.getOrDefault(dia, 0L), fechadosPorDia.getOrDefault(dia, 0L),
                    valorPorDia.getOrDefault(dia, BigDecimal.ZERO), tempoMedioPorDia.get(dia)));
            dia = dia.plusDays(1);
        }
        return serie;
    }

    private Map<LocalDate, Long> mapaPorDia(List<DiaContagemProjection> linhas) {
        return linhas.stream().collect(Collectors.toMap(DiaContagemProjection::getDia, DiaContagemProjection::getQuantidade));
    }

    private List<DashboardRankingResponse> montarRanking(DashboardFiltroRequest filtro) {
        List<DashboardRankingOportunidadeRow> oportunidadeRows = oportunidadeRepository.rankingOportunidadesPorUsuario(filtro);
        List<DashboardRankingProtocoloRow> protocoloRows = protocoloRepository.rankingProtocolosPorUsuario(filtro);

        Map<Long, String> nomesPorUsuario = new HashMap<>();
        Map<Long, DashboardRankingOportunidadeRow> oportunidadePorUsuario = new HashMap<>();
        Map<Long, DashboardRankingProtocoloRow> protocoloPorUsuario = new HashMap<>();

        for (DashboardRankingOportunidadeRow row : oportunidadeRows) {
            nomesPorUsuario.put(row.usuarioId(), row.usuarioNome());
            oportunidadePorUsuario.put(row.usuarioId(), row);
        }
        for (DashboardRankingProtocoloRow row : protocoloRows) {
            nomesPorUsuario.put(row.usuarioId(), row.usuarioNome());
            protocoloPorUsuario.put(row.usuarioId(), row);
        }

        List<DashboardRankingResponse> ranking = new ArrayList<>(nomesPorUsuario.size());
        for (Map.Entry<Long, String> entry : nomesPorUsuario.entrySet()) {
            Long usuarioId = entry.getKey();
            DashboardRankingOportunidadeRow op = oportunidadePorUsuario.get(usuarioId);
            DashboardRankingProtocoloRow pr = protocoloPorUsuario.get(usuarioId);

            long ganhas = op != null ? op.oportunidadesGanhas() : 0L;
            long perdidas = op != null ? op.oportunidadesPerdidas() : 0L;
            BigDecimal valorVendido = op != null ? op.valorVendido() : BigDecimal.ZERO;
            long protocolosFechados = pr != null ? pr.protocolosFechados() : 0L;
            Double tempoMedio = pr != null ? pr.tempoMedioAtendimentoMinutos() : null;

            ranking.add(new DashboardRankingResponse(usuarioId, entry.getValue(), protocolosFechados, tempoMedio,
                    ganhas, perdidas, valorVendido, calcularTaxaConversao(ganhas, perdidas)));
        }

        ranking.sort(Comparator.comparing(DashboardRankingResponse::valorVendido).reversed());
        return ranking;
    }

    private DashboardCadenciaResponse montarCadencias(DashboardFiltroRequest filtro) {
        long ativas = cadenciaFunilRepository.countBySituacao(filtro, Situacao.ATIVA);
        long pausadas = cadenciaFunilRepository.countBySituacao(filtro, Situacao.INATIVA);

        List<CadenciaFunil> cadenciasAtivas = cadenciaFunilRepository.findAtivasVisiveis(filtro);
        List<Long> etapasOrigem = cadenciasAtivas.stream().map(c -> c.getEtapaOrigem().getId()).distinct().toList();
        List<Long> etapasDestino = cadenciasAtivas.stream().map(c -> c.getEtapaDestino().getId()).distinct().toList();

        long oportunidadesEmExecucao = etapasOrigem.isEmpty() ? 0L : oportunidadeRepository.countPorEtapaIdIn(etapasOrigem);

        LocalDateTime inicioHoje = LocalDate.now().atStartOfDay();
        LocalDateTime fimHoje = inicioHoje.plusDays(1);
        long execucoesHoje = etapasDestino.isEmpty() ? 0L
                : logMovimentacaoCadenciaRepository.countByExecutadoEmBetweenAndEtapaDestinoIdIn(inicioHoje, fimHoje, etapasDestino);

        return new DashboardCadenciaResponse(ativas, pausadas, execucoesHoje, oportunidadesEmExecucao, calcularProximaExecucao(cadenciasAtivas));
    }

    private LocalDateTime calcularProximaExecucao(List<CadenciaFunil> cadenciasAtivas) {
        LocalDateTime agora = LocalDateTime.now(FUSO_HORARIO_MOVIMENTACAO);
        LocalDate hoje = agora.toLocalDate();
        LocalTime horaAtual = agora.toLocalTime();

        LocalDateTime proxima = null;
        for (CadenciaFunil cadencia : cadenciasAtivas) {
            LocalTime horario = cadencia.getHorarioMovimentacao();
            if (horario == null) {
                continue;
            }
            LocalDateTime candidata = horario.isAfter(horaAtual)
                    ? LocalDateTime.of(hoje, horario)
                    : LocalDateTime.of(hoje.plusDays(1), horario);
            if (proxima == null || candidata.isBefore(proxima)) {
                proxima = candidata;
            }
        }
        return proxima;
    }

    private DashboardResponse dashboardVazio() {
        DashboardSummaryResponse summary = new DashboardSummaryResponse(BigDecimal.ZERO, null, 0.0, 0L, 0L, null, null, 0L, 0L, 0L);
        DashboardCadenciaResponse cadencias = new DashboardCadenciaResponse(0L, 0L, 0L, 0L, null);
        return new DashboardResponse(summary, List.of(), List.of(), List.of(), List.of(), cadencias);
    }
}

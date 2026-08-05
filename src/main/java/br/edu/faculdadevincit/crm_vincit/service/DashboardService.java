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
import br.edu.faculdadevincit.crm_vincit.model.enums.Origem;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import br.edu.faculdadevincit.crm_vincit.model.enums.SituacaoOportunidade;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import br.edu.faculdadevincit.crm_vincit.repository.CadenciaFunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.FunilRepository;
import br.edu.faculdadevincit.crm_vincit.repository.OportunidadeRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ProtocoloRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ProtocoloRepository.DiaContagemProjection;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final int PERIODO_PADRAO_DIAS = 30;

    @Autowired
    private ProtocoloRepository protocoloRepository;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private CadenciaFunilRepository cadenciaFunilRepository;

    @Autowired
    private FunilRepository funilRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${dashboard.protocolo.risco-horas:24}")
    private long protocoloRiscoHoras;

    @Cacheable(value = CacheConfig.DASHBOARD_CACHE,
            key = "T(org.springframework.security.core.context.SecurityContextHolder).context.authentication.name" +
                    " + '|' + #startDateParam + '|' + #endDateParam + '|' + #pipelineId + '|' + #userIdFiltro" +
                    " + '|' + #status + '|' + #origin + '|' + #tagIds")
    public DashboardResponse getDashboard(LocalDateTime startDateParam, LocalDateTime endDateParam, Long pipelineId,
                                           Long userIdFiltro, SituacaoOportunidade status, Origem origin, List<Long> tagIds) {
        Usuario usuario = getUsuarioAutenticado();

        LocalDateTime endDate = endDateParam != null ? endDateParam : LocalDateTime.now();
        LocalDateTime startDate = startDateParam != null ? startDateParam : endDate.minusDays(PERIODO_PADRAO_DIAS);

        Long userId = resolverUserIdComAutorizacao(usuario, userIdFiltro);
        List<Long> funilIdsPermitidos = resolverFunilIdsPermitidos(usuario, pipelineId);
        if (funilIdsPermitidos.isEmpty()) {
            return dashboardVazio();
        }

        List<Long> tagsNormalizadas = tagIds != null ? tagIds : List.of();
        DashboardFiltroRequest filtro = new DashboardFiltroRequest(
                startDate, endDate, pipelineId, userId, status, origin, tagsNormalizadas, funilIdsPermitidos);

        return new DashboardResponse(
                montarSummary(filtro),
                montarFunil(filtro),
                montarOrigens(filtro),
                montarSerieDiaria(filtro),
                montarRanking(filtro),
                montarCadencias(filtro));
    }

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();
        return (Usuario) usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    private Long resolverUserIdComAutorizacao(Usuario usuario, Long userIdFiltro) {
        if (usuario.getCargo() == UserRole.ADMINISTRADOR) {
            return userIdFiltro;
        }
        if (userIdFiltro != null && !userIdFiltro.equals(usuario.getId())) {
            throw new AccessDeniedException("Você só pode consultar seus próprios indicadores.");
        }
        return usuario.getId();
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
        BigDecimal valorAtual = oportunidadeRepository.sumValorPorSituacao(filtro, SituacaoOportunidade.ABERTO);
        BigDecimal valorAnterior = oportunidadeRepository.sumValorPorSituacao(filtroPeriodoAnterior(filtro), SituacaoOportunidade.ABERTO);
        Double variacaoPercentual = calcularVariacaoPercentual(valorAtual, valorAnterior);

        long oportunidadesAbertas = oportunidadeRepository.countPorSituacao(filtro, SituacaoOportunidade.ABERTO);
        long oportunidadesGanhas = oportunidadeRepository.countPorSituacao(filtro, SituacaoOportunidade.GANHO);
        long oportunidadesPerdidas = oportunidadeRepository.countPorSituacao(filtro, SituacaoOportunidade.PERDIDO);
        Double taxaConversao = calcularTaxaConversao(oportunidadesGanhas, oportunidadesPerdidas);

        long protocolosAbertos = protocoloRepository.countAbertos(filtro);
        LocalDateTime limiteRisco = LocalDateTime.now().minusHours(protocoloRiscoHoras);
        long protocolosEmRisco = protocoloRepository.countEmRisco(filtro, limiteRisco);
        Double tempoMedioAtendimentoMinutos = protocoloRepository.avgTempoAtendimentoMinutos(filtro);

        return new DashboardSummaryResponse(valorAtual, variacaoPercentual, taxaConversao, protocolosAbertos,
                protocolosEmRisco, tempoMedioAtendimentoMinutos, oportunidadesAbertas, oportunidadesGanhas, oportunidadesPerdidas);
    }

    private DashboardFiltroRequest filtroPeriodoAnterior(DashboardFiltroRequest filtro) {
        Duration duracao = Duration.between(filtro.getStartDate(), filtro.getEndDate());
        LocalDateTime endAnterior = filtro.getStartDate();
        LocalDateTime startAnterior = endAnterior.minus(duracao);
        return new DashboardFiltroRequest(startAnterior, endAnterior, filtro.getPipelineId(), filtro.getUserId(),
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
        Map<LocalDate, Long> abertosPorDia = mapaPorDia(
                protocoloRepository.countAbertosPorDia(filtro.getStartDate(), filtro.getEndDate(), filtro.getUserId()));
        Map<LocalDate, Long> fechadosPorDia = mapaPorDia(
                protocoloRepository.countFechadosPorDia(filtro.getStartDate(), filtro.getEndDate(), filtro.getUserId()));

        List<DashboardSerieDiariaResponse> serie = new ArrayList<>();
        LocalDate dia = filtro.getStartDate().toLocalDate();
        LocalDate ultimoDia = filtro.getEndDate().toLocalDate();
        while (!dia.isAfter(ultimoDia)) {
            serie.add(new DashboardSerieDiariaResponse(dia, abertosPorDia.getOrDefault(dia, 0L), fechadosPorDia.getOrDefault(dia, 0L)));
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
                : oportunidadeRepository.countMovidasNoDiaPorEtapaIdIn(etapasDestino, inicioHoje, fimHoje);

        return new DashboardCadenciaResponse(ativas, pausadas, execucoesHoje, oportunidadesEmExecucao, calcularProximaExecucao(cadenciasAtivas));
    }

    private LocalDateTime calcularProximaExecucao(List<CadenciaFunil> cadenciasAtivas) {
        LocalDateTime agora = LocalDateTime.now();
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
        DashboardSummaryResponse summary = new DashboardSummaryResponse(BigDecimal.ZERO, null, 0.0, 0L, 0L, null, 0L, 0L, 0L);
        DashboardCadenciaResponse cadencias = new DashboardCadenciaResponse(0L, 0L, 0L, 0L, null);
        return new DashboardResponse(summary, List.of(), List.of(), List.of(), List.of(), cadencias);
    }
}

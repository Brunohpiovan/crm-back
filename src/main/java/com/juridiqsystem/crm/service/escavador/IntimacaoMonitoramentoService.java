package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.infra.escavador.EscavadorMonitoramentoDiarioApi;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorAparicao;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMonitoramentoDiarioResponse;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorOrigemDiario;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorOrigemGrupo;
import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.Empresa;
import com.juridiqsystem.crm.model.IntimacaoMonitoramento;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.IntimacaoMonitoramentoResponse;
import com.juridiqsystem.crm.model.dtos.MonitoramentoQuotaResponse;
import com.juridiqsystem.crm.model.dtos.escavador.IntimacaoInput;
import com.juridiqsystem.crm.model.enums.Uf;
import com.juridiqsystem.crm.repository.EmpresaRepository;
import com.juridiqsystem.crm.repository.IntimacaoMonitoramentoRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.IntimacaoService;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import com.juridiqsystem.crm.service.exceptions.EscavadorApiException;
import com.juridiqsystem.crm.service.exceptions.IntegrationException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Liga e desliga o monitoramento contínuo de uma OAB nos Diários Oficiais (Escavador API v1),
 * respeitando a cota de OABs monitoradas simultaneamente do plano da empresa. Mirror ponto a
 * ponto de {@link ProcessoMonitoramentoService}, mas por OAB em vez de por processo — ver
 * IntimacaoMonitoramento.
 *
 * <p>A ativação grava a intenção (linha com ativo = true) e só então cria a assinatura na
 * Escavador. Se a criação falhar por erro transitório, a linha permanece com
 * escavadorMonitoramentoId nulo e o IntimacaoMonitoramentoScheduler reconcilia depois — é por
 * isso que o retorno expõe confirmadoNaEscavador separado de ativo.</p>
 */
@Slf4j
@Service
public class IntimacaoMonitoramentoService {

    @Autowired
    private IntimacaoMonitoramentoRepository intimacaoMonitoramentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private EscavadorMonitoramentoDiarioApi escavadorMonitoramentoDiarioApi;

    @Autowired
    private IntimacaoService intimacaoService;

    @Transactional
    public IntimacaoMonitoramentoResponse ativar(String oabNumero, Uf oabUf, String usuarioAdvogadoPublicId) {
        String numeroNormalizado = oabNumero == null ? null : oabNumero.trim();
        Usuario usuarioAdvogado = buscarUsuarioAdvogado(usuarioAdvogadoPublicId);
        Optional<IntimacaoMonitoramento> existente = intimacaoMonitoramentoRepository.findByOabNumeroAndOabUf(numeroNormalizado, oabUf);

        if (existente.isPresent() && Boolean.TRUE.equals(existente.get().getAtivo())) {
            // Já ativa: chamar de novo só troca o advogado vinculado (informativo) — não consome
            // outra vaga da cota nem recria a assinatura na Escavador (diferente do monitoramento
            // de processo, a OAB em si nunca muda aqui, só quem é o dono do vínculo).
            IntimacaoMonitoramento monitoramento = existente.get();
            monitoramento.setUsuarioAdvogado(usuarioAdvogado);
            intimacaoMonitoramentoRepository.save(monitoramento);
            return IntimacaoMonitoramentoResponse.from(monitoramento);
        }

        Long empresaId = TenantContext.get();
        verificarCota(empresaId);

        String origemIds = resolverOrigemIds(oabUf);
        IntimacaoMonitoramento monitoramento = existente.orElseGet(IntimacaoMonitoramento::new);
        monitoramento.ativar(numeroNormalizado, oabUf, usuarioAdvogado, origemIds, usuarioAutenticado());
        intimacaoMonitoramentoRepository.save(monitoramento);

        criarNaEscavador(monitoramento);
        return IntimacaoMonitoramentoResponse.from(monitoramento);
    }

    @Transactional
    public void desativar(Long id) {
        IntimacaoMonitoramento monitoramento = intimacaoMonitoramentoRepository.findById(id)
                .filter(m -> Boolean.TRUE.equals(m.getAtivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Esta OAB não está sendo monitorada."));

        if (monitoramento.getEscavadorMonitoramentoId() != null) {
            // Falha aqui NÃO desliga localmente de propósito: uma assinatura que continua viva na
            // Escavador segue custando e enviando callbacks, então é melhor o usuário ver o erro e
            // tentar de novo do que ficar com os dois lados divergentes.
            removerNaEscavador(monitoramento.getEscavadorMonitoramentoId());
        }
        monitoramento.desativar();
        intimacaoMonitoramentoRepository.save(monitoramento);
    }

    /**
     * empresaId não é usado no corpo do método de propósito: o filtro de tenant do Hibernate
     * (@TenantId em IntimacaoMonitoramento) já restringe findAllByOrderByAtivadoEmDesc à empresa
     * do TenantContext atual. O parâmetro é mantido na assinatura para simetria com
     * obterCotaAtual(Long) e com o padrão já usado pelo controller (TenantContext.get() explícito
     * na chamada), não porque seja necessário para a query.
     */
    @Transactional(readOnly = true)
    public List<IntimacaoMonitoramentoResponse> listar(Long empresaId) {
        return intimacaoMonitoramentoRepository.findAllByOrderByAtivadoEmDesc().stream()
                .map(IntimacaoMonitoramentoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MonitoramentoQuotaResponse obterCotaAtual(Long empresaId) {
        return new MonitoramentoQuotaResponse(
                limiteDaEmpresa(empresaId),
                intimacaoMonitoramentoRepository.countByEmpresaIdAndAtivoTrue(empresaId));
    }

    /**
     * Confirma na Escavador uma ativação que ficou pendente. Chamado pelo scheduler de
     * reconciliação, já dentro do tenant correto.
     */
    @Transactional
    public void confirmarPendente(Long monitoramentoId) {
        IntimacaoMonitoramento monitoramento = intimacaoMonitoramentoRepository.findById(monitoramentoId)
                .filter(m -> Boolean.TRUE.equals(m.getAtivo()) && m.getEscavadorMonitoramentoId() == null)
                .orElse(null);
        if (monitoramento == null) {
            return; // desligado ou já confirmado entre a leitura do scheduler e agora
        }
        criarNaEscavador(monitoramento);
        intimacaoMonitoramentoRepository.save(monitoramento);
    }

    /**
     * Rede de segurança complementar ao callback em tempo real: recupera, via
     * GET /api/v1/monitoramentos/{id}/aparicoes (grátis), publicações que a Escavador já detectou
     * mas cujo callback não chegou (webhook fora do ar, URL pública indisponível, reentregas
     * esgotadas etc.). Chamada tanto manualmente (botão "Sincronizar" por OAB) quanto por
     * IntimacaoMonitoramentoScheduler.sincronizarTodasAtivas (diariamente, para todas as OABs
     * confirmadas). Idempotente: rodar de novo sobre as mesmas aparições não duplica nada — ver
     * IntimacaoService.registrarDoCallback / construirChaveDedupe.
     *
     * @return quantas aparições eram realmente novas (não deduplicadas).
     */
    @Transactional
    public int sincronizarAparicoes(Long monitoramentoId) {
        IntimacaoMonitoramento monitoramento = intimacaoMonitoramentoRepository.findById(monitoramentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Esta OAB não está sendo monitorada."));
        if (monitoramento.getEscavadorMonitoramentoId() == null) {
            // Ainda "confirmando..." — não há assinatura na Escavador pra consultar ainda.
            return 0;
        }

        List<EscavadorAparicao> aparicoes = escavadorMonitoramentoDiarioApi.listarAparicoes(monitoramento.getEscavadorMonitoramentoId());
        int novas = 0;
        for (EscavadorAparicao aparicao : aparicoes) {
            IntimacaoInput input = new IntimacaoInput(
                    null, null, null, parseDataPublicacao(aparicao.dataPublicacao()), aparicao.conteudo(), null,
                    null, null, null, aparicao.id());
            if (intimacaoService.registrarDoCallback(monitoramento, input)) {
                novas++;
            }
        }
        return novas;
    }

    private LocalDate parseDataPublicacao(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(data.trim().substring(0, Math.min(10, data.trim().length())));
        } catch (Exception e) {
            return null;
        }
    }

    private void criarNaEscavador(IntimacaoMonitoramento monitoramento) {
        try {
            List<Integer> origemIds = parseOrigemIds(monitoramento.getOrigemIds());
            EscavadorMonitoramentoDiarioResponse resposta = escavadorMonitoramentoDiarioApi.criar(monitoramento.getOabNumero(), origemIds);
            if (resposta == null || resposta.id() == null) {
                throw new EscavadorApiException("Escavador não devolveu o id do monitoramento criado.");
            }
            monitoramento.setEscavadorMonitoramentoId(String.valueOf(resposta.id()));
        } catch (EscavadorApiException e) {
            // Intencionalmente não propaga: a linha já está gravada como ativa e o scheduler de
            // reconciliação retenta. O usuário vê o card em "confirmando..." em vez de um erro para
            // uma falha que costuma ser transitória.
            log.warn("Falha ao criar monitoramento de OAB na Escavador. oabNumero={} oabUf={} monitoramentoId={}",
                    monitoramento.getOabNumero(), monitoramento.getOabUf(), monitoramento.getId(), e);
        }
    }

    private void removerNaEscavador(String escavadorMonitoramentoId) {
        try {
            escavadorMonitoramentoDiarioApi.remover(escavadorMonitoramentoId);
        } catch (EscavadorApiException e) {
            throw new IntegrationException("Não foi possível desligar o monitoramento na Escavador. Tente novamente em alguns instantes.", e);
        }
    }

    private void verificarCota(Long empresaId) {
        Integer limite = limiteDaEmpresa(empresaId);
        if (limite == null) {
            return; // sem limite configurado = ilimitado
        }
        long utilizados = intimacaoMonitoramentoRepository.countByEmpresaIdAndAtivoTrue(empresaId);
        if (utilizados >= limite) {
            throw new ConflictException(
                    "Limite de %d OABs monitoradas do plano atingido. Desligue o monitoramento de outra OAB ou contrate mais.".formatted(limite));
        }
    }

    private Integer limiteDaEmpresa(Long empresaId) {
        // Não usar .map(Empresa::getIntimacoesMonitoradasLimite).orElseThrow(...): quando o limite
        // é null (plano sem cota configurada = ilimitado), Optional.map colapsa para
        // Optional.empty() e o orElseThrow dispararia "Empresa não encontrada" para uma empresa
        // que existe.
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
        return empresa.getIntimacoesMonitoradasLimite();
    }

    private Usuario buscarUsuarioAdvogado(String usuarioAdvogadoPublicId) {
        if (usuarioAdvogadoPublicId == null || usuarioAdvogadoPublicId.isBlank()) {
            return null; // vínculo é opcional, ver seção de integração leve com o Prompt 2
        }
        return usuarioRepository.findByPublicId(usuarioAdvogadoPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário (advogado) não encontrado."));
    }

    private Usuario usuarioAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        throw new ResourceNotFoundException("Usuário autenticado não encontrado.");
    }

    /**
     * Heurística: todos os diários cujo {@code estado} bate com a UF da OAB, mais os de âmbito
     * nacional/superior. A API não documenta um flag explícito de "nacional" — tratamos
     * {@code estado} nulo/vazio, ou {@code categoria} contendo "Superior"/"Tribunais Superiores",
     * como indício de âmbito nacional. **Precisa ser validada contra a resposta real de
     * GET /api/v1/origens numa conta de produção** antes de confiar cegamente nela; um seletor
     * manual de diários fica como evolução futura caso a heurística se mostre imprecisa.
     */
    private String resolverOrigemIds(Uf oabUf) {
        List<EscavadorOrigemGrupo> grupos = escavadorMonitoramentoDiarioApi.listarOrigens();
        List<Integer> ids = grupos.stream()
                .flatMap(grupo -> grupo.diarios() == null ? Stream.<EscavadorOrigemDiario>empty() : grupo.diarios().stream())
                .filter(diario -> pertenceAUfOuNacional(diario, oabUf))
                .map(EscavadorOrigemDiario::id)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private boolean pertenceAUfOuNacional(EscavadorOrigemDiario diario, Uf oabUf) {
        String estado = diario.estado();
        if (estado == null || estado.isBlank()) {
            return true; // heurística best-effort: sem estado = âmbito nacional/superior
        }
        if (estado.equalsIgnoreCase(oabUf.name())) {
            return true;
        }
        String categoria = diario.categoria();
        return categoria != null && (categoria.contains("Superior") || categoria.contains("Tribunais Superiores"));
    }

    private List<Integer> parseOrigemIds(String origemIdsCsv) {
        if (origemIdsCsv == null || origemIdsCsv.isBlank()) {
            return List.of();
        }
        return Stream.of(origemIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .toList();
    }
}

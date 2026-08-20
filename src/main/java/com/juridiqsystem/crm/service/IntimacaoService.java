package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Intimacao;
import com.juridiqsystem.crm.model.IntimacaoMonitoramento;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.dtos.IntimacaoResponse;
import com.juridiqsystem.crm.model.dtos.PageResponse;
import com.juridiqsystem.crm.model.dtos.escavador.IntimacaoInput;
import com.juridiqsystem.crm.repository.IntimacaoRepository;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.escavador.NovaIntimacaoDetectadaEvent;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orquestra o registro idempotente de intimações (publicações em Diário Oficial) vindas do
 * callback de monitoramento de OAB, e a listagem/marcação de lida consumida pela página
 * Intimações. Mesmo papel de ProcessoDocumentoService para o módulo de processos — pacote raiz
 * {@code service} (não {@code service.escavador}), mesmo lugar de ProcessoDocumentoService.
 *
 * <p>Sem self-proxy: diferente de ProcessoDocumentoService (que precisa de um proxy autoinjetado
 * porque um método público não-transacional da própria classe chama o método transacional), aqui
 * {@code registrarDoCallback} é @Transactional diretamente — é chamado só por
 * EscavadorCallbackService (uma classe diferente), então a interceptação via proxy do Spring já
 * funciona normalmente, sem precisar da indireção de self-injection.</p>
 */
@Service
public class IntimacaoService {

    private final IntimacaoRepository intimacaoRepository;
    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ApplicationEventPublisher eventPublisher;

    public IntimacaoService(IntimacaoRepository intimacaoRepository,
                             ProcessoRepository processoRepository,
                             UsuarioRepository usuarioRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.intimacaoRepository = intimacaoRepository;
        this.processoRepository = processoRepository;
        this.usuarioRepository = usuarioRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Chamado por EscavadorCallbackService ao receber um callback diario_movimentacao_nova ou
     * diario_citacao_nova, uma vez por IntimacaoMonitoramento correspondente (uma publicação pode
     * citar mais de uma das nossas OABs), e por IntimacaoMonitoramentoService.sincronizarAparicoes
     * ao recuperar aparições via GET .../aparicoes. Idempotente por chaveDedupe: a Escavador
     * reenvia o mesmo callback até 11 vezes quando a entrega falha, e a sincronização pode ser
     * chamada várias vezes sobre as mesmas aparições.
     *
     * @return true se uma nova Intimacao foi criada; false se já existia (dedupe) — usado pela
     *         sincronização para reportar quantas aparições eram realmente novas.
     */
    @Transactional
    public boolean registrarDoCallback(IntimacaoMonitoramento monitoramento, IntimacaoInput input) {
        String chaveDedupe = construirChaveDedupe(monitoramento.getId(), input);
        if (intimacaoRepository.existsByChaveDedupe(chaveDedupe)) {
            return false;
        }

        Intimacao intimacao = new Intimacao();
        intimacao.setIntimacaoMonitoramento(monitoramento);
        intimacao.setProcesso(resolverProcesso(monitoramento.getEmpresaId(), input.numeroCnjIdentificado()));
        intimacao.setNumeroCnjIdentificado(input.numeroCnjIdentificado());
        intimacao.setDiarioNome(input.diarioNome());
        intimacao.setDiarioSigla(input.diarioSigla());
        intimacao.setDiarioData(input.diarioData());
        intimacao.setConteudo(input.conteudo());
        intimacao.setLink(input.link());
        intimacao.setChaveDedupe(chaveDedupe);
        intimacao.setCriadoEm(LocalDateTime.now());
        intimacaoRepository.save(intimacao);

        eventPublisher.publishEvent(new NovaIntimacaoDetectadaEvent(
                intimacao.getId(), monitoramento.getEmpresaId(), monitoramento.getOabNumero(), intimacao.getDiarioNome()));
        return true;
    }

    /**
     * @param usuarioAdvogadoPublicId publicId do advogado (Usuario), como recebido do frontend —
     *                                resolvido aqui para o id interno antes de filtrar. Um
     *                                publicId que não existe é tratado como "nenhum resultado" em
     *                                vez de erro: é só um filtro, não uma operação crítica.
     */
    @Transactional(readOnly = true)
    public PageResponse<IntimacaoResponse> listar(Boolean lida, String usuarioAdvogadoPublicId, Pageable pageable) {
        Long usuarioAdvogadoId = null;
        if (usuarioAdvogadoPublicId != null && !usuarioAdvogadoPublicId.isBlank()) {
            Long resolvido = usuarioRepository.findByPublicId(usuarioAdvogadoPublicId).map(u -> u.getId()).orElse(null);
            if (resolvido == null) {
                return new PageResponse<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
            }
            usuarioAdvogadoId = resolvido;
        }

        Page<Intimacao> pagina = intimacaoRepository.buscar(lida, usuarioAdvogadoId, pageable);
        return new PageResponse<>(
                pagina.getContent().stream().map(IntimacaoResponse::from).toList(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }

    @Transactional
    public void marcarComoLida(Long id) {
        Intimacao intimacao = intimacaoRepository.findByIdComMonitoramento(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intimação não encontrada."));
        intimacao.marcarComoLida();
        intimacaoRepository.save(intimacao);
    }

    /**
     * Natural key com prioridade em três níveis, para sobreviver tanto a reentrega de callback (até
     * 11x) quanto a rodar a sincronização manual/agendada (sincronizarAparicoes) mais de uma vez
     * sobre a mesma aparição — ver comentário na entidade Intimacao e em IntimacaoInput.
     *
     * <p>1) {@code escavadorAparicaoId}: vem de sincronizarAparicoes, é o id estável da aparição na
     * Escavador — dedupe exato, inclusive entre execuções repetidas da sincronização.<br>
     * 2) {@code diarioId}+{@code pagina}: vem de callback, quando o payload traz esses campos.<br>
     * 3) fallback pelo uuid do callback (estável entre reentregas do mesmo evento) ou, na ausência
     * de qualquer identificador estável, um uuid aleatório (não dedupe, mas nunca quebra o insert).
     *
     * <p>Prefixado pelo id do monitoramento em todos os casos, para não colidir quando a mesma
     * publicação cita mais de uma das nossas OABs. Uma mesma publicação real pode, no limite,
     * acabar registrada duas vezes se chegar por callback E por sincronização antes que a primeira
     * seja processada pela outra via (não há um identificador compartilhado entre os dois
     * formatos) — limitação aceita: melhor um duplicado raro do que perder a intimação.
     */
    private String construirChaveDedupe(Long intimacaoMonitoramentoId, IntimacaoInput input) {
        if (input.escavadorAparicaoId() != null) {
            return intimacaoMonitoramentoId + ":ap:" + input.escavadorAparicaoId();
        }
        if (input.diarioId() != null && input.pagina() != null) {
            return intimacaoMonitoramentoId + ":" + input.diarioId() + ":" + input.pagina();
        }
        String fallback = input.uuidCallback() != null && !input.uuidCallback().isBlank()
                ? input.uuidCallback()
                : java.util.UUID.randomUUID().toString();
        return intimacaoMonitoramentoId + ":" + fallback;
    }

    private Processo resolverProcesso(Long empresaId, String numeroCnj) {
        if (numeroCnj == null || numeroCnj.isBlank()) {
            return null;
        }
        return processoRepository.findByEmpresaIdAndNumeroCnj(empresaId, numeroCnj).orElse(null);
    }
}

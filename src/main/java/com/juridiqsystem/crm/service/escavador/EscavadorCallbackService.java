package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackMapper;
import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackProperties;
import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackTokenValidator;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorCallbackPayload;
import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.EscavadorCallbackEvento;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.ProcessoMonitoramento;
import com.juridiqsystem.crm.model.dtos.escavador.MovimentacaoInput;
import com.juridiqsystem.crm.repository.EscavadorCallbackEventoRepository;
import com.juridiqsystem.crm.repository.ProcessoMonitoramentoRepository;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.service.ProcessoDocumentoService;
import com.juridiqsystem.crm.service.ProcessoMovimentacaoService;
import com.juridiqsystem.crm.service.exceptions.AccessDeniedException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Processa os callbacks que a Escavador envia quando há novidade em um processo monitorado.
 * Mesmo formato de responsabilidade de WhatsAppService.receiveWebhookEvent: valida a origem
 * ANTES de qualquer resolução de tenant, resolve a empresa a partir do dado recebido e só então
 * roda a regra de negócio dentro de TenantContext.runAs.
 *
 * <p>Todo callback autenticado vira uma linha em escavador_callback_evento, inclusive quando o
 * processamento falha — é o registro que permite reprocessar manualmente e o único rastro de um
 * callback para um processo que não existe nesta instalação. Falha de processamento nunca vira
 * erro HTTP: a Escavador reenviaria o mesmo payload até 11 vezes, o que não resolve nada quando
 * a causa é permanente (processo inexistente), e para falhas transitórias existe o reenvio manual
 * pelo painel.</p>
 */
@Slf4j
@Service
public class EscavadorCallbackService {

    @Autowired
    private EscavadorCallbackTokenValidator tokenValidator;

    @Autowired
    private EscavadorCallbackProperties callbackProperties;

    @Autowired
    private EscavadorCallbackMapper callbackMapper;

    @Autowired
    private EscavadorCallbackEventoRepository escavadorCallbackEventoRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private ProcessoMonitoramentoRepository processoMonitoramentoRepository;

    @Autowired
    private ProcessoMovimentacaoService processoMovimentacaoService;

    @Autowired
    private ProcessoDocumentoService processoDocumentoService;

    @Autowired
    private ProcessoMonitoramentoService processoMonitoramentoService;

    public void receber(String rawBody, String authorizationHeader, String tokenQueryParam) {
        if (!tokenValidator.isValid(authorizationHeader, tokenQueryParam, callbackProperties.getToken())) {
            // Sem gravar o payload: um endpoint público que persiste tudo que chega, mesmo não
            // autenticado, é um vetor de enchimento de banco.
            throw new AccessDeniedException("Callback da Escavador rejeitado: token ausente ou inválido.");
        }

        EscavadorCallbackEvento evento = escavadorCallbackEventoRepository.save(new EscavadorCallbackEvento(rawBody));
        String numeroCnj = null;
        try {
            EscavadorCallbackPayload payload = callbackMapper.parse(rawBody);
            numeroCnj = payload.numeroCnj();
            processar(payload);
            evento.marcarProcessado(numeroCnj);
        } catch (RuntimeException e) {
            log.error("Falha ao processar callback da Escavador. eventoId={} numeroCnj={}", evento.getId(), numeroCnj, e);
            evento.marcarErro(numeroCnj, e.getMessage());
        }
        escavadorCallbackEventoRepository.save(evento);
    }

    private void processar(EscavadorCallbackPayload payload) {
        if (payload.isNovaMovimentacao()) {
            registrarMovimentacoes(payload);
        } else if (payload.isNovoDocumento()) {
            registrarDocumento(payload);
        } else if (payload.isProcessoNaoEncontrado()) {
            desligarMonitoramentoSemProcessoNoTribunal(payload);
        } else {
            // processo_verificado, processo_encontrado, novo_processo... não alteram estado no
            // CRM hoje; ficam registrados no evento bruto para auditoria.
            log.debug("Callback da Escavador ignorado (evento sem efeito no CRM). event={}", payload.event());
        }
    }

    private void registrarMovimentacoes(EscavadorCallbackPayload payload) {
        List<MovimentacaoInput> movimentacoes = callbackMapper.toMovimentacaoInputs(payload);
        if (movimentacoes.isEmpty()) {
            throw new IllegalStateException("Callback de nova movimentação sem conteúdo utilizável.");
        }
        aplicarNosMonitoradosDoCnj(payload.numeroCnj(), (processo, monitoramento) ->
                processoMovimentacaoService.registrarMovimentacoes(processo.getId(), movimentacoes));
    }

    /** Só chega quando o monitoramento foi ligado com "Incluir documentos públicos". */
    private void registrarDocumento(EscavadorCallbackPayload payload) {
        if (payload.documento() == null) {
            throw new IllegalStateException("Callback de novo documento sem objeto documento.");
        }
        aplicarNosMonitoradosDoCnj(payload.numeroCnj(), (processo, monitoramento) ->
                processoDocumentoService.registrarDoCallback(processo.getId(), payload.documento()));
    }

    /**
     * A Escavador não achou o processo no sistema do tribunal, então não vai monitorá-lo nem
     * cobrar por ele. Manter a linha como ativa faria a empresa ver "monitorando" para algo que
     * ninguém está acompanhando, e ainda ocuparia uma vaga da cota do plano.
     */
    private void desligarMonitoramentoSemProcessoNoTribunal(EscavadorCallbackPayload payload) {
        aplicarNosMonitoradosDoCnj(payload.numeroCnj(), (processo, monitoramento) -> {
            log.warn("Escavador nao localizou o processo no tribunal; desligando monitoramento. numeroCnj={} monitoramentoId={}",
                    payload.numeroCnj(), monitoramento.getId());
            processoMonitoramentoService.desligarPorProcessoNaoEncontrado(monitoramento.getId());
        });
    }

    /**
     * O callback identifica o processo só pelo número CNJ, que é único por empresa e não
     * globalmente: duas empresas clientes podem, de forma legítima, monitorar o mesmo processo
     * público. Por isso a ação roda para cada empresa que tenha monitoramento ativo daquele CNJ —
     * atender só a primeira faria as demais pagarem a cota e nunca receberem nada.
     *
     * <p>Empresas que apenas consultaram o processo, sem monitoramento ativo, ficam de fora: elas
     * não estão pagando por acompanhamento contínuo.</p>
     */
    private void aplicarNosMonitoradosDoCnj(String numeroCnj, BiConsumer<Processo, ProcessoMonitoramento> acao) {
        List<Processo> processos = Optional.ofNullable(numeroCnj)
                .map(processoRepository::findAllByNumeroCnjIgnoringTenant)
                .orElseGet(List::of);
        if (processos.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Callback recebido para processo não cadastrado nesta instalação. numeroCnj=" + numeroCnj);
        }

        int atendidos = 0;
        for (Processo processo : processos) {
            // O webhook é público e roda sem sessão: o tenant vem do próprio processo encontrado.
            Boolean aplicou = TenantContext.runAs(processo.getEmpresaId(), () ->
                    processoMonitoramentoRepository.findByProcessoId(processo.getId())
                            .filter(monitoramento -> Boolean.TRUE.equals(monitoramento.getAtivo()))
                            .map(monitoramento -> {
                                acao.accept(processo, monitoramento);
                                return true;
                            })
                            .orElse(false));
            if (Boolean.TRUE.equals(aplicou)) {
                atendidos++;
            }
        }

        if (atendidos == 0) {
            // Não é erro: pode ser corrida com um monitoramento recém-desligado. Mas se persistir,
            // é sinal de assinatura órfã na Escavador (custo sem contrapartida) — daí o warn.
            log.warn("Callback da Escavador sem nenhum monitoramento ativo correspondente. numeroCnj={}", numeroCnj);
        }
    }
}

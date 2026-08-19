package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackMapper;
import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackProperties;
import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackTokenValidator;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorCallbackPayload;
import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.EscavadorCallbackEvento;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.dtos.escavador.MovimentacaoInput;
import com.juridiqsystem.crm.repository.EscavadorCallbackEventoRepository;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.service.ProcessoMovimentacaoService;
import com.juridiqsystem.crm.service.exceptions.AccessDeniedException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
    private ProcessoMovimentacaoService processoMovimentacaoService;

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
        if (!payload.isNovaMovimentacao()) {
            // Os demais eventos (processo_verificado, processo_encontrado, novo_documento...) não
            // alteram estado no CRM hoje; ficam registrados no evento bruto para auditoria.
            log.debug("Callback da Escavador ignorado (evento sem efeito no CRM). event={}", payload.event());
            return;
        }

        List<MovimentacaoInput> movimentacoes = callbackMapper.toMovimentacaoInputs(payload);
        if (movimentacoes.isEmpty()) {
            throw new IllegalStateException("Callback de nova movimentação sem conteúdo utilizável.");
        }

        Processo processo = buscarProcesso(payload.numeroCnj());
        // O webhook é público e roda sem sessão: o tenant vem do próprio processo encontrado.
        TenantContext.runAs(processo.getEmpresaId(), () ->
                processoMovimentacaoService.registrarMovimentacoes(processo.getId(), movimentacoes));
    }

    private Processo buscarProcesso(String numeroCnj) {
        return Optional.ofNullable(numeroCnj)
                .flatMap(processoRepository::findByNumeroCnjIgnoringTenant)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Callback recebido para processo não cadastrado nesta instalação. numeroCnj=" + numeroCnj));
    }
}

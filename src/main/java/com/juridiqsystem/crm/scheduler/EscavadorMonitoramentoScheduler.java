package com.juridiqsystem.crm.scheduler;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.ProcessoMonitoramento;
import com.juridiqsystem.crm.repository.ProcessoMonitoramentoRepository;
import com.juridiqsystem.crm.service.escavador.ProcessoMonitoramentoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reconcilia monitoramentos que o usuário ligou mas cuja assinatura na Escavador não chegou a ser
 * criada (falha transitória de rede/API no momento da ativação — ver
 * ProcessoMonitoramentoService.criarNaEscavador, que grava a intenção e não propaga o erro).
 *
 * <p>Arquivo separado do MovimentacaoScheduler de propósito: outro domínio e outra cadência.
 * De hora em hora é suficiente — a ativação já tentou uma vez de forma síncrona, isto aqui é a
 * rede de segurança, não o caminho normal.</p>
 */
@Slf4j
@Component
public class EscavadorMonitoramentoScheduler {

    /**
     * Teto por execução para não transformar uma indisponibilidade prolongada da Escavador em uma
     * rajada de milhares de chamadas (e de cobranças) na hora em que ela voltar.
     */
    private static final int LIMITE_POR_EXECUCAO = 50;

    @Autowired
    private ProcessoMonitoramentoRepository processoMonitoramentoRepository;

    @Autowired
    private ProcessoMonitoramentoService processoMonitoramentoService;

    @Scheduled(cron = "0 15 * * * *") // a cada hora, aos 15 minutos
    public void reconciliarPendentes() {
        try {
            List<ProcessoMonitoramento> pendentes =
                    processoMonitoramentoRepository.findPendentesDeConfirmacaoIgnoringTenant(LIMITE_POR_EXECUCAO);
            if (pendentes.isEmpty()) {
                return;
            }
            log.info("Reconciliando {} monitoramento(s) pendente(s) de confirmação na Escavador.", pendentes.size());
            pendentes.forEach(this::confirmar);
        } catch (Exception e) {
            log.error("Execução do EscavadorMonitoramentoScheduler falhou de forma inesperada.", e);
        }
    }

    /**
     * O scheduler roda fora de qualquer requisição HTTP, então não há tenant resolvido: cada item
     * é confirmado dentro do tenant lido da própria linha, senão as escritas cairiam no tenant
     * sentinela e nada seria atualizado.
     */
    private void confirmar(ProcessoMonitoramento pendente) {
        try {
            TenantContext.runAs(pendente.getEmpresaId(), () -> {
                processoMonitoramentoService.confirmarPendente(pendente.getId());
                return null;
            });
        } catch (RuntimeException e) {
            // Falha isolada por item: um processo com número inválido não pode impedir a
            // reconciliação dos demais. O próximo tick tenta de novo.
            log.warn("Falha ao reconciliar monitoramento pendente. monitoramentoId={}", pendente.getId(), e);
        }
    }
}

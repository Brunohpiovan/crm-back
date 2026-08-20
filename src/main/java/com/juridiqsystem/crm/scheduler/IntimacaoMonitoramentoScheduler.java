package com.juridiqsystem.crm.scheduler;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.IntimacaoMonitoramento;
import com.juridiqsystem.crm.repository.IntimacaoMonitoramentoRepository;
import com.juridiqsystem.crm.service.escavador.IntimacaoMonitoramentoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reconcilia OABs monitoradas que o usuário ligou mas cuja assinatura na Escavador não chegou a
 * ser criada (falha transitória de rede/API no momento da ativação — ver
 * IntimacaoMonitoramentoService.criarNaEscavador, que grava a intenção e não propaga o erro).
 * Mirror de EscavadorMonitoramentoScheduler.
 *
 * <p>Arquivo próprio, cadência própria: roda aos 45 minutos de cada hora (0 45 * * * *),
 * deliberadamente num offset diferente de EscavadorMonitoramentoScheduler (0 15 * * * *) para não
 * concentrar os dois lotes de retentativa contra a Escavador no mesmo minuto.</p>
 */
@Slf4j
@Component
public class IntimacaoMonitoramentoScheduler {

    /**
     * Teto por execução — mesmo racional de EscavadorMonitoramentoScheduler.LIMITE_POR_EXECUCAO:
     * não transformar uma indisponibilidade prolongada da Escavador numa rajada de chamadas (e de
     * cobranças) na hora em que ela voltar.
     */
    private static final int LIMITE_POR_EXECUCAO = 50;

    @Autowired
    private IntimacaoMonitoramentoRepository intimacaoMonitoramentoRepository;

    @Autowired
    private IntimacaoMonitoramentoService intimacaoMonitoramentoService;

    @Scheduled(cron = "0 45 * * * *") // a cada hora, aos 45 minutos
    public void reconciliarPendentes() {
        try {
            List<IntimacaoMonitoramento> pendentes =
                    intimacaoMonitoramentoRepository.findPendentesDeConfirmacaoIgnoringTenant(LIMITE_POR_EXECUCAO);
            if (pendentes.isEmpty()) {
                return;
            }
            log.info("Reconciliando {} OAB(s) monitorada(s) pendente(s) de confirmação na Escavador.", pendentes.size());
            pendentes.forEach(this::confirmar);
        } catch (Exception e) {
            log.error("Execução do IntimacaoMonitoramentoScheduler falhou de forma inesperada.", e);
        }
    }

    /**
     * O scheduler roda fora de qualquer requisição HTTP, então não há tenant resolvido: cada item
     * é confirmado dentro do tenant lido da própria linha, senão as escritas cairiam no tenant
     * sentinela e nada seria atualizado.
     */
    private void confirmar(IntimacaoMonitoramento pendente) {
        try {
            TenantContext.runAs(pendente.getEmpresaId(), () -> {
                intimacaoMonitoramentoService.confirmarPendente(pendente.getId());
                return null;
            });
        } catch (RuntimeException e) {
            // Falha isolada por item: uma OAB com termo inválido não pode impedir a reconciliação
            // das demais. O próximo tick tenta de novo.
            log.warn("Falha ao reconciliar OAB monitorada pendente. monitoramentoId={}", pendente.getId(), e);
        }
    }

    /**
     * Rede de segurança complementar ao callback em tempo real: sincroniza (via
     * IntimacaoMonitoramentoService.sincronizarAparicoes, grátis) as aparições de toda OAB ativa e
     * confirmada, recuperando o que um callback silenciosamente perdido (webhook fora do ar no
     * momento em que a Escavador tentou entregar) deixaria de aparecer no CRM. Diária, não horária
     * como {@link #reconciliarPendentes()}: aparições não são tão urgentes quanto confirmar uma
     * assinatura pendente, e rodar isso a cada hora para toda OAB ativa seria uma chamada
     * desnecessariamente frequente contra a Escavador (mesmo sendo grátis).
     */
    @Scheduled(cron = "0 30 3 * * *") // diariamente às 3h30 — fora do horário comercial
    public void sincronizarTodasAtivas() {
        try {
            List<IntimacaoMonitoramento> ativas =
                    intimacaoMonitoramentoRepository.findAtivasConfirmadasIgnoringTenant(LIMITE_POR_EXECUCAO);
            if (ativas.isEmpty()) {
                return;
            }
            log.info("Sincronizando aparições de {} OAB(s) monitorada(s) ativa(s).", ativas.size());
            ativas.forEach(this::sincronizar);
        } catch (Exception e) {
            log.error("Sincronização diária de aparições do IntimacaoMonitoramentoScheduler falhou de forma inesperada.", e);
        }
    }

    private void sincronizar(IntimacaoMonitoramento ativa) {
        try {
            TenantContext.runAs(ativa.getEmpresaId(), () -> {
                int novas = intimacaoMonitoramentoService.sincronizarAparicoes(ativa.getId());
                if (novas > 0) {
                    log.info("Sincronização recuperou {} intimação(ões) que não chegaram por callback. monitoramentoId={}",
                            novas, ativa.getId());
                }
                return null;
            });
        } catch (RuntimeException e) {
            // Falha isolada por item: mesmo racional de confirmar(...) acima.
            log.warn("Falha ao sincronizar aparições de OAB monitorada. monitoramentoId={}", ativa.getId(), e);
        }
    }
}

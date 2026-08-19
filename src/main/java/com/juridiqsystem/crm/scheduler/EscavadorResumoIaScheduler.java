package com.juridiqsystem.crm.scheduler;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.service.ProcessoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Checa em background os processos com resumo por IA pendente (solicitado mas ainda não confirmado
 * FINALIZADO pela Escavador) — sem isso, o usuário só saberia que terminou se mantivesse a tela do
 * processo aberta e ficasse tentando de novo (ver ProcessoService.gerarOuObterResumoIa). Quando
 * confirma, persiste o resumo e ResumoIaNotificacaoService avisa o frontend via WebSocket, de
 * qualquer tela do sistema.
 *
 * <p>Cadência de 20s (vs. hora em hora do EscavadorMonitoramentoScheduler): aqui é o caminho normal
 * de conclusão, não uma rede de segurança para falha transitória — a geração real leva até ~1-2min,
 * então precisa de checagem frequente para a notificação parecer "em tempo real".</p>
 */
@Slf4j
@Component
public class EscavadorResumoIaScheduler {

    /** Teto por execução — mesmo racional de EscavadorMonitoramentoScheduler.LIMITE_POR_EXECUCAO. */
    private static final int LIMITE_POR_EXECUCAO = 50;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private ProcessoService processoService;

    @Scheduled(fixedDelay = 20_000)
    public void verificarPendentes() {
        try {
            List<Processo> pendentes = processoRepository.findComResumoIaPendenteIgnoringTenant(LIMITE_POR_EXECUCAO);
            if (pendentes.isEmpty()) {
                return;
            }
            pendentes.forEach(this::verificar);
        } catch (Exception e) {
            log.error("Execução do EscavadorResumoIaScheduler falhou de forma inesperada.", e);
        }
    }

    /** Roda fora de requisição HTTP: cada item é verificado dentro do tenant lido da própria linha. */
    private void verificar(Processo pendente) {
        try {
            TenantContext.runAs(pendente.getEmpresaId(), () -> {
                processoService.verificarResumoIaPendente(pendente.getPublicId());
                return null;
            });
        } catch (RuntimeException e) {
            // Falha isolada por item: um erro na Escavador para um processo não pode impedir a
            // checagem dos demais pendentes. O próximo tick (20s depois) tenta de novo.
            log.warn("Falha ao verificar resumo IA pendente. processoId={}", pendente.getId(), e);
        }
    }
}

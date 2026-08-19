package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.infra.escavador.EscavadorRequisicaoRealizadaEvent;
import com.juridiqsystem.crm.model.EscavadorCreditoLancamento;
import com.juridiqsystem.crm.repository.EscavadorCreditoLancamentoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ledger interno de consumo da conta única do juriq-crm na Escavador: grava uma linha por chamada
 * feita à API, atribuída à empresa que a originou. Serve para auditoria e para detectar gasto
 * anormal — não é a unidade vendida ao cliente (essa é a cota de processos monitorados).
 */
@Slf4j
@Service
public class EscavadorCreditoService {

    @Autowired
    private EscavadorCreditoLancamentoRepository escavadorCreditoLancamentoRepository;

    /**
     * REQUIRES_NEW porque o lançamento precisa sobreviver ao rollback de quem originou a chamada:
     * a Escavador cobra pela requisição mesmo quando a transação de negócio falha depois, e um
     * ledger que "esquece" justamente os gastos com erro não serve para detectar gasto anormal.
     *
     * <p>Nenhuma falha aqui pode derrubar o fluxo que fez a chamada — o ledger é observabilidade,
     * não regra de negócio.</p>
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(EscavadorRequisicaoRealizadaEvent evento) {
        try {
            escavadorCreditoLancamentoRepository.save(new EscavadorCreditoLancamento(
                    evento.empresaId(), evento.endpoint(), evento.custoCentavos(), evento.sucesso()));
        } catch (RuntimeException e) {
            log.error("Falha ao registrar lançamento de crédito da Escavador. empresaId={} endpoint={}",
                    evento.empresaId(), evento.endpoint(), e);
        }
    }
}

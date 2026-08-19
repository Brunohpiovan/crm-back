package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.infra.escavador.EscavadorMonitoramentoApi;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMonitoramentoResponse;
import com.juridiqsystem.crm.model.Empresa;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.ProcessoMonitoramento;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.MonitoramentoQuotaResponse;
import com.juridiqsystem.crm.model.dtos.ProcessoMonitoramentoDetailResponse;
import com.juridiqsystem.crm.model.enums.FrequenciaMonitoramento;
import com.juridiqsystem.crm.repository.EmpresaRepository;
import com.juridiqsystem.crm.repository.ProcessoMonitoramentoRepository;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import com.juridiqsystem.crm.service.exceptions.EscavadorApiException;
import com.juridiqsystem.crm.service.exceptions.IntegrationException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Liga e desliga o monitoramento contínuo de um processo na Escavador, respeitando a cota de
 * processos monitorados simultaneamente do plano da empresa.
 *
 * <p>A ativação grava a intenção (linha com ativo = true) e só então cria a assinatura na
 * Escavador. Se a criação falhar por erro transitório, a linha permanece com
 * escavadorMonitoramentoId nulo e o EscavadorMonitoramentoScheduler reconcilia depois — é por
 * isso que o retorno expõe confirmadoNaEscavador separado de ativo.</p>
 */
@Slf4j
@Service
public class ProcessoMonitoramentoService {

    @Autowired
    private ProcessoMonitoramentoRepository processoMonitoramentoRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private EscavadorMonitoramentoApi escavadorMonitoramentoApi;

    @Transactional
    public ProcessoMonitoramentoDetailResponse ativar(String processoPublicId, FrequenciaMonitoramento frequencia, boolean comDocumentos) {
        Processo processo = buscarProcesso(processoPublicId);
        Optional<ProcessoMonitoramento> existente = processoMonitoramentoRepository.findByProcessoId(processo.getId());

        if (existente.isPresent() && naMesmaConfiguracao(existente.get(), frequencia, comDocumentos)) {
            return ProcessoMonitoramentoDetailResponse.from(existente.get());
        }

        boolean jaConsumiaCota = existente.map(m -> Boolean.TRUE.equals(m.getAtivo())).orElse(false);
        if (!jaConsumiaCota) {
            verificarCota(processo.getEmpresaId());
        }

        // Trocar de frequência/documentos exige recriar a assinatura: a Escavador não edita um
        // monitoramento de processo já criado (só o de novos processos tem PATCH).
        existente.filter(m -> m.getEscavadorMonitoramentoId() != null)
                .ifPresent(m -> removerNaEscavador(m.getEscavadorMonitoramentoId()));

        ProcessoMonitoramento monitoramento = existente.orElseGet(ProcessoMonitoramento::new);
        monitoramento.setProcesso(processo);
        monitoramento.ativar(frequencia, comDocumentos, usuarioAutenticado());
        processoMonitoramentoRepository.save(monitoramento);

        criarNaEscavador(monitoramento, processo.getNumeroCnj());
        return ProcessoMonitoramentoDetailResponse.from(monitoramento);
    }

    @Transactional
    public void desativar(String processoPublicId) {
        Processo processo = buscarProcesso(processoPublicId);
        ProcessoMonitoramento monitoramento = processoMonitoramentoRepository.findByProcessoId(processo.getId())
                .filter(m -> Boolean.TRUE.equals(m.getAtivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Este processo não está sendo monitorado."));

        if (monitoramento.getEscavadorMonitoramentoId() != null) {
            // Falha aqui NÃO desliga localmente de propósito: um monitoramento que continua vivo
            // na Escavador segue custando e enviando callbacks, então é melhor o usuário ver o
            // erro e tentar de novo do que ficar com os dois lados divergentes.
            removerNaEscavador(monitoramento.getEscavadorMonitoramentoId());
        }
        monitoramento.desativar();
        processoMonitoramentoRepository.save(monitoramento);
    }

    @Transactional(readOnly = true)
    public ProcessoMonitoramentoDetailResponse obter(String processoPublicId) {
        Processo processo = buscarProcesso(processoPublicId);
        return processoMonitoramentoRepository.findByProcessoId(processo.getId())
                .map(ProcessoMonitoramentoDetailResponse::from)
                .orElseGet(ProcessoMonitoramentoDetailResponse::inativo);
    }

    @Transactional(readOnly = true)
    public MonitoramentoQuotaResponse obterCotaAtual(Long empresaId) {
        return new MonitoramentoQuotaResponse(
                limiteDaEmpresa(empresaId),
                processoMonitoramentoRepository.countByEmpresaIdAndAtivoTrue(empresaId));
    }

    /**
     * Confirma na Escavador uma ativação que ficou pendente. Chamado pelo scheduler de
     * reconciliação, já dentro do tenant correto.
     */
    @Transactional
    public void confirmarPendente(Long monitoramentoId) {
        ProcessoMonitoramento monitoramento = processoMonitoramentoRepository.findById(monitoramentoId)
                .filter(m -> Boolean.TRUE.equals(m.getAtivo()) && m.getEscavadorMonitoramentoId() == null)
                .orElse(null);
        if (monitoramento == null) {
            return; // desligado ou já confirmado entre a leitura do scheduler e agora
        }
        criarNaEscavador(monitoramento, monitoramento.getProcesso().getNumeroCnj());
        processoMonitoramentoRepository.save(monitoramento);
    }

    /**
     * Desliga um monitoramento porque a Escavador informou, por callback, que não localizou o
     * processo no tribunal. Diferente de {@link #desativar(String)}, NÃO chama a Escavador: a
     * assinatura já não existe do lado de lá (e não houve cobrança). Libera a vaga na cota.
     */
    @Transactional
    public void desligarPorProcessoNaoEncontrado(Long monitoramentoId) {
        processoMonitoramentoRepository.findById(monitoramentoId)
                .filter(m -> Boolean.TRUE.equals(m.getAtivo()))
                .ifPresent(monitoramento -> {
                    monitoramento.desativar();
                    processoMonitoramentoRepository.save(monitoramento);
                });
    }

    private void criarNaEscavador(ProcessoMonitoramento monitoramento, String numeroCnj) {
        try {
            EscavadorMonitoramentoResponse resposta = escavadorMonitoramentoApi.criar(
                    numeroCnj, monitoramento.getFrequencia(), Boolean.TRUE.equals(monitoramento.getComDocumentos()));
            if (resposta == null || resposta.id() == null) {
                throw new EscavadorApiException("Escavador não devolveu o id do monitoramento criado.");
            }
            monitoramento.setEscavadorMonitoramentoId(String.valueOf(resposta.id()));
        } catch (EscavadorApiException e) {
            // Intencionalmente não propaga: a linha já está gravada como ativa e o scheduler de
            // reconciliação retenta. O usuário vê o card em "confirmando..." em vez de um erro
            // para uma falha que costuma ser transitória.
            log.warn("Falha ao criar monitoramento na Escavador. numeroCnj={} monitoramentoId={}",
                    numeroCnj, monitoramento.getId(), e);
        }
    }

    private void removerNaEscavador(String escavadorMonitoramentoId) {
        try {
            escavadorMonitoramentoApi.remover(escavadorMonitoramentoId);
        } catch (EscavadorApiException e) {
            throw new IntegrationException("Não foi possível desligar o monitoramento na Escavador. Tente novamente em alguns instantes.", e);
        }
    }

    private void verificarCota(Long empresaId) {
        Integer limite = limiteDaEmpresa(empresaId);
        if (limite == null) {
            return; // sem limite configurado = ilimitado (mesmo espírito de protocoloRiscoHoras)
        }
        long utilizados = processoMonitoramentoRepository.countByEmpresaIdAndAtivoTrue(empresaId);
        if (utilizados >= limite) {
            throw new ConflictException(
                    "Limite de %d processos monitorados do plano atingido. Desligue o monitoramento de outro processo ou contrate mais.".formatted(limite));
        }
    }

    private Integer limiteDaEmpresa(Long empresaId) {
        // Não usar .map(Empresa::getProcessosMonitoradosLimite).orElseThrow(...): quando o limite é
        // null (plano sem cota configurada = ilimitado), Optional.map colapsa para Optional.empty()
        // e o orElseThrow dispararia "Empresa não encontrada" para uma empresa que existe.
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada."));
        return empresa.getProcessosMonitoradosLimite();
    }

    private Processo buscarProcesso(String processoPublicId) {
        return processoRepository.findByPublicId(processoPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado."));
    }

    private Usuario usuarioAutenticado() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        throw new ResourceNotFoundException("Usuário autenticado não encontrado.");
    }

    private boolean naMesmaConfiguracao(ProcessoMonitoramento monitoramento, FrequenciaMonitoramento frequencia, boolean comDocumentos) {
        return Boolean.TRUE.equals(monitoramento.getAtivo())
                && monitoramento.getFrequencia() == frequencia
                && Boolean.TRUE.equals(monitoramento.getComDocumentos()) == comDocumentos;
    }
}

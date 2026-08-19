package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.ProcessoMovimentacao;
import com.juridiqsystem.crm.model.dtos.escavador.MovimentacaoInput;
import com.juridiqsystem.crm.repository.ProcessoMovimentacaoRepository;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.service.escavador.NovasMovimentacoesDetectadasEvent;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Único ponto de entrada de movimentações no sistema (consulta manual e, no Prompt 2, callback de
 * monitoramento) — ver contrato compartilhado no prompt de implementação. Idempotente via
 * (processo_id, data_movimentacao, hash_conteudo); publica NovasMovimentacoesDetectadasEvent
 * somente quando pelo menos uma movimentação nova é persistida.
 */
@Service
public class ProcessoMovimentacaoService {

    private final ProcessoRepository processoRepository;
    private final ProcessoMovimentacaoRepository processoMovimentacaoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProcessoMovimentacaoService(ProcessoRepository processoRepository,
                                        ProcessoMovimentacaoRepository processoMovimentacaoRepository,
                                        ApplicationEventPublisher eventPublisher) {
        this.processoRepository = processoRepository;
        this.processoMovimentacaoRepository = processoMovimentacaoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public List<ProcessoMovimentacao> registrarMovimentacoes(Long processoId, List<MovimentacaoInput> novas) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo com id " + processoId + " nao encontrado"));

        List<ProcessoMovimentacao> persistidas = new ArrayList<>();
        for (MovimentacaoInput input : novas) {
            String hash = hash(normalizar(input.conteudo()));
            boolean jaExiste = processoMovimentacaoRepository
                    .existsByProcessoIdAndDataMovimentacaoAndHashConteudo(processoId, input.dataMovimentacao(), hash);
            if (jaExiste) {
                continue;
            }
            ProcessoMovimentacao movimentacao = new ProcessoMovimentacao();
            movimentacao.setProcesso(processo);
            movimentacao.setDataMovimentacao(input.dataMovimentacao());
            movimentacao.setTipo(input.tipo());
            movimentacao.setConteudo(input.conteudo());
            movimentacao.setHashConteudo(hash);
            movimentacao.setFonte(input.fonte());
            movimentacao.setCriadoEm(LocalDateTime.now());
            persistidas.add(processoMovimentacaoRepository.save(movimentacao));
        }

        if (!persistidas.isEmpty()) {
            eventPublisher.publishEvent(new NovasMovimentacoesDetectadasEvent(
                    processo.getId(),
                    processo.getPublicId(),
                    processo.getEmpresaId(),
                    persistidas.stream().map(ProcessoMovimentacao::getId).toList()));
        }
        return persistidas;
    }

    private String normalizar(String conteudo) {
        if (conteudo == null) {
            return "";
        }
        return conteudo.strip().toLowerCase().replaceAll("\\s+", " ");
    }

    private String hash(String textoNormalizado) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(textoNormalizado.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel na JVM", e);
        }
    }
}

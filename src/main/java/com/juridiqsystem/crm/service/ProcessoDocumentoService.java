package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.escavador.EscavadorProcessoApi;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorCallbackDocumento;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorDocumento;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorDocumentoPaginado;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.ProcessoDocumento;
import com.juridiqsystem.crm.model.dtos.ProcessoDocumentoResponse;
import com.juridiqsystem.crm.model.dtos.escavador.DocumentoInput;
import com.juridiqsystem.crm.model.enums.FonteDocumento;
import com.juridiqsystem.crm.repository.ProcessoDocumentoRepository;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.service.escavador.NovoDocumentoDetectadoEvent;
import com.juridiqsystem.crm.service.exceptions.EscavadorApiException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestra consulta de documentos públicos na Escavador (busca sob demanda, chamada paga
 * separada da consulta de capa — §8.4) + upsert local, e o registro do documento vindo do callback
 * de monitoramento {@code novo_documento}. Mesmo racional de ProcessoService/
 * ProcessoMovimentacaoService: chamada de rede fora de transação, upsert idempotente pela
 * constraint única (processo_id, escavador_documento_id).
 */
@Service
public class ProcessoDocumentoService {

    private final EscavadorProcessoApi escavadorProcessoApi;
    private final ProcessoRepository processoRepository;
    private final ProcessoDocumentoRepository processoDocumentoRepository;
    private final ApplicationEventPublisher eventPublisher;
    /** Proxy autoinjetado — mesmo motivo/padrão de ProcessoService.self (@Transactional só funciona via proxy AOP). */
    private final ProcessoDocumentoService self;

    public ProcessoDocumentoService(EscavadorProcessoApi escavadorProcessoApi,
                                     ProcessoRepository processoRepository,
                                     ProcessoDocumentoRepository processoDocumentoRepository,
                                     ApplicationEventPublisher eventPublisher,
                                     @org.springframework.context.annotation.Lazy ProcessoDocumentoService self) {
        this.escavadorProcessoApi = escavadorProcessoApi;
        this.processoRepository = processoRepository;
        this.processoDocumentoRepository = processoDocumentoRepository;
        this.eventPublisher = eventPublisher;
        this.self = self;
    }

    /** Busca completa dos documentos públicos disponíveis na Escavador para o CNJ e upsert local (idempotente). */
    public List<ProcessoDocumentoResponse> buscarEUpsertar(String processoPublicId) {
        Processo processo = buscarProcesso(processoPublicId);
        EscavadorDocumentoPaginado paginado = buscarIgnorandoNaoEncontrado(processo.getNumeroCnj());
        if (paginado != null && paginado.items() != null && !paginado.items().isEmpty()) {
            List<DocumentoInput> entradas = paginado.items().stream()
                    .map(doc -> mapInput(doc, FonteDocumento.BUSCA_MANUAL))
                    .filter(input -> input != null)
                    .toList();
            self.registrarDocumentos(processo.getId(), entradas);
        }
        return listar(processoPublicId);
    }

    /** Chamado por EscavadorCallbackService ao receber um callback novo_documento. */
    public void registrarDoCallback(Long processoId, EscavadorCallbackDocumento documento) {
        DocumentoInput input = mapInput(documento, FonteDocumento.CALLBACK_MONITORAMENTO);
        if (input == null) {
            return;
        }
        self.registrarDocumentos(processoId, List.of(input));
    }

    public List<ProcessoDocumentoResponse> listar(String processoPublicId) {
        Processo processo = buscarProcesso(processoPublicId);
        return processoDocumentoRepository.findByProcessoIdOrderByDataDocumentoDesc(processo.getId())
                .stream()
                .map(ProcessoDocumentoResponse::new)
                .toList();
    }

    /** Proxy de download: nunca expõe o token da Escavador nem a URL temporária ao frontend. */
    public byte[] baixarPdf(String processoPublicId, Long documentoId) {
        Processo processo = buscarProcesso(processoPublicId);
        ProcessoDocumento documento = processoDocumentoRepository.findByIdAndProcessoId(documentoId, processo.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado neste processo."));
        return escavadorProcessoApi.baixarDocumentoPdf(processo.getNumeroCnj(), documento.getEscavadorDocumentoId());
    }

    /**
     * Ponto de entrada único e idempotente de documentos, usado tanto por busca manual quanto por
     * callback de monitoramento — mesmo papel de ProcessoMovimentacaoService.registrarMovimentacoes.
     * Publica NovoDocumentoDetectadoEvent para cada documento novo persistido.
     */
    @Transactional
    public List<ProcessoDocumento> registrarDocumentos(Long processoId, List<DocumentoInput> novos) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo com id " + processoId + " nao encontrado"));

        List<ProcessoDocumento> persistidos = new ArrayList<>();
        for (DocumentoInput input : novos) {
            boolean jaExiste = processoDocumentoRepository
                    .existsByProcessoIdAndEscavadorDocumentoId(processoId, input.escavadorDocumentoId());
            if (jaExiste) {
                continue;
            }
            ProcessoDocumento documento = new ProcessoDocumento();
            documento.setProcesso(processo);
            documento.setEscavadorDocumentoId(input.escavadorDocumentoId());
            documento.setTitulo(input.titulo());
            documento.setDescricao(input.descricao());
            documento.setDataDocumento(input.dataDocumento());
            documento.setTipo(input.tipo());
            documento.setExtensaoArquivo(input.extensaoArquivo());
            documento.setQuantidadePaginas(input.quantidadePaginas());
            documento.setFonte(input.fonte());
            documento.setCriadoEm(LocalDateTime.now());
            persistidos.add(processoDocumentoRepository.save(documento));
        }

        for (ProcessoDocumento documento : persistidos) {
            eventPublisher.publishEvent(new NovoDocumentoDetectadoEvent(
                    processo.getId(), processo.getPublicId(), processo.getEmpresaId(), documento.getTitulo()));
        }
        return persistidos;
    }

    /** 404 num endpoint sem nenhum documento ainda é "lista vazia", não erro — mesmo racional de ProcessoService.buscarIgnorandoNaoEncontrado. */
    private EscavadorDocumentoPaginado buscarIgnorandoNaoEncontrado(String numeroCnj) {
        try {
            return escavadorProcessoApi.listarDocumentos(numeroCnj);
        } catch (EscavadorApiException e) {
            if (e.getStatusHttp() == 404) {
                return null;
            }
            throw e;
        }
    }

    private Processo buscarProcesso(String processoPublicId) {
        return processoRepository.findByPublicId(processoPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo com id " + processoPublicId + " nao encontrado"));
    }

    private DocumentoInput mapInput(EscavadorDocumento documento, FonteDocumento fonte) {
        String escavadorDocumentoId = documento.key() != null ? documento.key() : String.valueOf(documento.id());
        if (escavadorDocumentoId == null || escavadorDocumentoId.isBlank()) {
            return null;
        }
        return new DocumentoInput(escavadorDocumentoId, documento.titulo(), documento.descricao(),
                parseData(documento.data()), documento.tipo(), documento.extensaoArquivo(),
                documento.quantidadePaginas(), fonte);
    }

    private DocumentoInput mapInput(EscavadorCallbackDocumento documento, FonteDocumento fonte) {
        String escavadorDocumentoId = documento.key() != null ? documento.key() : String.valueOf(documento.id());
        if (escavadorDocumentoId == null || escavadorDocumentoId.isBlank()) {
            return null;
        }
        return new DocumentoInput(escavadorDocumentoId, documento.titulo(), documento.descricao(),
                parseData(documento.data()), documento.tipo(), documento.extensaoArquivo(),
                documento.quantidadePaginas(), fonte);
    }

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(data.trim().substring(0, Math.min(10, data.trim().length())));
        } catch (Exception e) {
            return null;
        }
    }
}

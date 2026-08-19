package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.ProcessoDocumentoResponse;
import com.juridiqsystem.crm.service.ProcessoDocumentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Processo Documento", description = "Documentos públicos de um processo, via Escavador Business API (v2).")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/processos/{publicId}/documentos")
public class ProcessoDocumentoController {

    @Autowired
    private ProcessoDocumentoService processoDocumentoService;

    @Operation(summary = "Listar documentos já salvos localmente",
            description = "Requer JWT. Não consulta a Escavador novamente — só documentos já obtidos por busca manual ou callback de monitoramento.")
    @GetMapping
    public List<ProcessoDocumentoResponse> listar(@PathVariable String publicId) {
        return processoDocumentoService.listar(publicId);
    }

    @Operation(summary = "Buscar documentos públicos na Escavador",
            description = "Requer JWT. Chamada paga separada da consulta de capa (§8.4) — busca todos os documentos públicos disponíveis para o CNJ e faz upsert local (idempotente).")
    @PostMapping("/buscar")
    public List<ProcessoDocumentoResponse> buscar(@PathVariable String publicId) {
        return processoDocumentoService.buscarEUpsertar(publicId);
    }

    @Operation(summary = "Download do PDF de um documento",
            description = "Requer JWT. Proxy: nunca expõe o token da Escavador nem a URL temporária ao frontend.")
    @GetMapping("/{documentoId}/pdf")
    public ResponseEntity<byte[]> baixarPdf(
            @Parameter(description = "publicId do processo", required = true) @PathVariable String publicId,
            @PathVariable Long documentoId) {
        byte[] pdf = processoDocumentoService.baixarPdf(publicId, documentoId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"documento-" + documentoId + ".pdf\"")
                .body(pdf);
    }
}

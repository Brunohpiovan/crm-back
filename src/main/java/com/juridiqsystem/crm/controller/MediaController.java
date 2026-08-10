package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.service.AudioConvertor;
import com.juridiqsystem.crm.service.MediaService;
import com.juridiqsystem.crm.service.S3Service;
import com.juridiqsystem.crm.service.auth.SlidingWindowRateLimiter;
import com.juridiqsystem.crm.service.exceptions.TooManyRequestsException;
import io.jsonwebtoken.io.IOException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Collections;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

@Tag(name = "Mídia", description = "Upload de arquivos de mídia (áudio e anexos genéricos) para o S3.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
public class MediaController {

    // Upload custa armazenamento/banda (S3) e é um vetor clássico de abuso (encher o bucket,
    // martelar o endpoint) — limite generoso o bastante pra não atrapalhar uso normal (anexar
    // um arquivo em um template, mandar um áudio no chat), mas que barra automação/abuso.
    private static final int MAX_UPLOADS_PER_WINDOW = 30;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    @Autowired
    private S3Service s3Service;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private AudioConvertor audioConvertor;

    @Autowired
    private SlidingWindowRateLimiter rateLimiter;

    private void checkRateLimit(String namespace) {
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!rateLimiter.tryAcquire(namespace + ":" + actor, MAX_UPLOADS_PER_WINDOW, RATE_LIMIT_WINDOW)) {
            throw new TooManyRequestsException("Muitos uploads em um curto intervalo. Tente novamente em instantes.");
        }
    }

    @Operation(
            summary = "Upload de áudio",
            description = """
                    Recebe um arquivo de áudio (`Content-Type` deve começar com `audio/`), converte para \
                    o formato Ogg/Opus usando FFmpeg (via javacv) e envia o resultado convertido para o S3, \
                    em `audio/{nomeDoArquivo}`. Retorna um JSON `{"url": "..."}` com a URL pública do \
                    arquivo convertido no S3. Sujeito ao limite global de upload da aplicação \
                    (`spring.servlet.multipart.max-file-size=50MB`).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Áudio convertido e enviado com sucesso. Corpo da resposta: JSON `{\"url\": \"...\"}` com a URL pública do arquivo no S3."),
            @ApiResponse(responseCode = "400", description = "Arquivo enviado não é um áudio (Content-Type não começa com 'audio/')",
                    content = @Content(schema = @Schema(implementation = com.juridiqsystem.crm.model.dtos.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Falha inesperada na conversão (FFmpeg) ou no upload para o S3")
    })
    @PostMapping("/audio/upload")
    public ResponseEntity<?> uploadAudio(
            @Parameter(description = "Arquivo de áudio a ser convertido e armazenado (multipart)", required = true)
            @RequestParam("file") MultipartFile file) throws Exception {
        checkRateLimit("upload-audio");
        String fileUrl = audioConvertor.convertAndUpload(file);
        return ResponseEntity.ok(Collections.singletonMap("url", fileUrl));
    }

    @Operation(
            summary = "Upload de anexo genérico",
            description = """
                    Recebe uma imagem (jpg/jpeg/png/gif), documento (pdf/doc/docx/odt/xls/xlsx/ppt/pptx/txt) \
                    ou csv e envia \
                    para o S3, em uma subpasta escolhida conforme a extensão (`imagem/` ou `documentos/`). \
                    Extensões fora dessa lista são rejeitadas. Para imagens, o conteúdo real do arquivo \
                    (assinatura de bytes) também é conferido — não basta a extensão do nome. Limite de \
                    tamanho de 50MB. Retorna um JSON `{"url": "..."}` com a URL pública do arquivo no S3.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo enviado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo excede o limite de 50MB, extensão não permitida, ou conteúdo não corresponde a uma imagem válida",
                    content = @Content(schema = @Schema(implementation = com.juridiqsystem.crm.model.dtos.ApiResponse.class)))
    })
    @PostMapping("/anexo/upload")
    public ResponseEntity<?> uploadAnexo(
            @Parameter(description = "Arquivo a ser armazenado no S3 (multipart, limite de 50MB)", required = true)
            @RequestParam("file") MultipartFile file) {
        checkRateLimit("upload-anexo");
        String fileUrl = mediaService.uploadAnexo(file);
        return ResponseEntity.ok(Collections.singletonMap("url", fileUrl));
    }

}

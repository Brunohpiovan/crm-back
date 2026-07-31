package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.service.AudioConvertor;
import br.edu.faculdadevincit.crm_vincit.service.MediaService;
import br.edu.faculdadevincit.crm_vincit.service.S3Service;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

@Tag(name = "Mídia", description = "Upload de arquivos de mídia (áudio e anexos genéricos) para o S3.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
public class MediaController {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private AudioConvertor audioConvertor;

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
                    content = @Content(schema = @Schema(implementation = br.edu.faculdadevincit.crm_vincit.model.dtos.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Falha inesperada na conversão (FFmpeg) ou no upload para o S3")
    })
    @PostMapping("/audio/upload")
    public ResponseEntity<?> uploadAudio(
            @Parameter(description = "Arquivo de áudio a ser convertido e armazenado (multipart)", required = true)
            @RequestParam("file") MultipartFile file) throws Exception {
        String fileUrl = audioConvertor.convertAndUpload(file);
        return ResponseEntity.ok(Collections.singletonMap("url", fileUrl));
    }

    @Operation(
            summary = "Upload de anexo genérico",
            description = """
                    Recebe um arquivo qualquer (imagem, documento — pdf/docx/doc/txt — ou csv) e envia \
                    para o S3, em uma subpasta escolhida conforme a extensão (`imagem/` ou `documentos/`); \
                    outras extensões são enviadas na raiz. Limite de tamanho de 50MB (rejeitado com erro \
                    de negócio antes mesmo do upload ao S3). Retorna um JSON `{"url": "..."}` com a URL \
                    pública do arquivo no S3.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arquivo enviado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Arquivo excede o limite de 50MB",
                    content = @Content(schema = @Schema(implementation = br.edu.faculdadevincit.crm_vincit.model.dtos.ApiResponse.class)))
    })
    @PostMapping("/anexo/upload")
    public ResponseEntity<?> uploadAnexo(
            @Parameter(description = "Arquivo a ser armazenado no S3 (multipart, limite de 50MB)", required = true)
            @RequestParam("file") MultipartFile file) {
        String fileUrl = mediaService.uploadAnexo(file);
        return ResponseEntity.ok(Collections.singletonMap("url", fileUrl));
    }

}

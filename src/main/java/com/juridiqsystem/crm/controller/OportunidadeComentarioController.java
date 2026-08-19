package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.OportunidadeComentarioDTO;
import com.juridiqsystem.crm.service.OportunidadeComentarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Oportunidade Comentário", description = "Comentários de uma oportunidade (aba 'Comentários' do modal de edição), com texto e anexo de imagem opcional.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/oportunidade/{oportunidadeId}/comentarios")
public class OportunidadeComentarioController {

    @Autowired
    private OportunidadeComentarioService oportunidadeComentarioService;

    @Operation(summary = "Listar comentários de uma oportunidade (paginado)",
            description = "Requer JWT. Retorna, paginados, os comentários da oportunidade, do mais recente para o mais antigo. Tamanho de página padrão: 10.")
    @ApiResponse(responseCode = "200", description = "Página de comentários",
            content = @Content(schema = @Schema(implementation = OportunidadeComentarioDTO.class)))
    @GetMapping
    public Page<OportunidadeComentarioDTO> listar(
            @Parameter(description = "Id da oportunidade", required = true) @PathVariable String oportunidadeId,
            @Parameter(description = "Parâmetros de paginação (page, size)") @PageableDefault(size = 10) Pageable pageable) {
        return oportunidadeComentarioService.listar(oportunidadeId, pageable);
    }

    @Operation(summary = "Criar um comentário na oportunidade",
            description = "Requer JWT. Requisição multipart/form-data: `conteudo` é o texto do comentário e `file`, opcional, é uma imagem (PNG, JPG, JPEG, WEBP ou GIF, até 100 MB) anexada ao comentário. O autor é sempre o usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comentário criado com sucesso",
                    content = @Content(schema = @Schema(implementation = OportunidadeComentarioDTO.class))),
            @ApiResponse(responseCode = "400", description = "Oportunidade não encontrada, ou arquivo de anexo inválido (tamanho acima de 100 MB ou tipo de imagem não permitido)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Oportunidade com id 1 nao encontrada")))
    })
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<OportunidadeComentarioDTO> criar(
            @Parameter(description = "Id da oportunidade", required = true) @PathVariable String oportunidadeId,
            @Parameter(description = "Texto do comentário", required = true) @RequestParam("conteudo") String conteudo,
            @Parameter(description = "Arquivo de imagem opcional a ser anexado ao comentário") @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(oportunidadeComentarioService.criar(oportunidadeId, conteudo, file));
    }

    @Operation(summary = "Excluir um comentário",
            description = "Requer JWT. Só o autor do comentário ou um administrador podem excluir.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comentário excluído com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400", description = "Comentário não encontrado, ou usuário sem permissão para excluí-lo",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Você não tem permissão para excluir este comentário")))
    })
    @DeleteMapping("/{comentarioId}")
    public ResponseEntity<?> excluir(
            @Parameter(description = "Id da oportunidade", required = true) @PathVariable String oportunidadeId,
            @Parameter(description = "Id do comentário", required = true) @PathVariable String comentarioId) {
        oportunidadeComentarioService.excluir(comentarioId);
        return ResponseEntity.ok().build();
    }
}

package com.juridiqsystem.crm.controller;


import com.juridiqsystem.crm.model.dtos.TagDTO;
import com.juridiqsystem.crm.model.dtos.TagOportunidadeDTO;
import com.juridiqsystem.crm.model.dtos.TagRequestDTO;
import com.juridiqsystem.crm.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Tag", description = "Tags usadas para classificar oportunidades (ex.: categorizações comerciais). CRUD e listagem apenas das tags ativas.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/tag")
public class TagController {

    @Autowired
    private TagService tagService;

    @Operation(summary = "Listar todas as tags", description = "Requer JWT. Retorna todas as tags cadastradas, ativas ou não, ordenadas por nome.")
    @ApiResponse(responseCode = "200", description = "Lista de tags",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TagDTO.class))))
    @GetMapping
    public List<TagDTO> findAll() {
        return tagService.findAll();
    }

    @Operation(summary = "Listar apenas as tags ativas", description = "Requer JWT. Retorna, em formato reduzido, somente as tags com situação ATIVA — usado tipicamente para popular o seletor de tags ao criar/editar uma oportunidade.")
    @ApiResponse(responseCode = "200", description = "Lista de tags ativas",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TagOportunidadeDTO.class))))
    @GetMapping("/ativas")
    public List<TagOportunidadeDTO> findAllAtivas() {
        return tagService.findAllAtivas();
    }

    @Operation(summary = "Buscar tag por id", description = "Requer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tag encontrada",
                    content = @Content(schema = @Schema(implementation = TagDTO.class))),
            @ApiResponse(responseCode = "400", description = "Tag não encontrada para o id informado (resposta em JSON estruturado — StandardError)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Tag nao encontrada")))
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<TagDTO> findById(@Parameter(description = "Id da tag", required = true) @PathVariable String id){
        return ResponseEntity.ok(tagService.findById(id));
    }

    @Operation(summary = "Criar uma nova tag", description = "Requer JWT. Não é permitido criar duas tags com o mesmo nome. Corpo validado com Bean Validation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tag criada com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400",
                    description = "Erro de validação Bean Validation dos campos do corpo da requisição (mapa simples campo -> mensagem).",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"campo\": \"mensagem de erro\"}"))),
            @ApiResponse(responseCode = "409",
                    description = "Já existe uma tag com o mesmo nome. O serviço lança ResponseStatusException(HttpStatus.BAD_REQUEST, ...), tratado por um handler dedicado no ResourceExceptionHandler que responde em JSON estruturado (StandardError).",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.juridiqsystem.crm.service.exceptions.StandardError.class)))
    })
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TagRequestDTO tag) {
        tagService.create(tag);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Excluir uma tag", description = "Requer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tag apagada com sucesso",
                    content = @Content(schema = @Schema(type = "object", example = "{\"message\": \"Tag apagada com sucesso\"}"))),
            @ApiResponse(responseCode = "404", description = "Tag não encontrada para o id informado (resposta em JSON estruturado — StandardError)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.juridiqsystem.crm.service.exceptions.StandardError.class)))
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "Id da tag", required = true) @PathVariable String id) {
        tagService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Tag apagada com sucesso"));
    }

    @Operation(summary = "Atualizar uma tag", description = "Requer JWT. Não é permitido renomear para um nome já usado por outra tag. Corpo validado com Bean Validation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tag atualizada com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400",
                    description = "Erro de validação Bean Validation dos campos do corpo da requisição (mapa simples campo -> mensagem).",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"campo\": \"mensagem de erro\"}"))),
            @ApiResponse(responseCode = "404",
                    description = "Tag não encontrada para o id informado. Resposta em JSON estruturado (StandardError).",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.juridiqsystem.crm.service.exceptions.StandardError.class))),
            @ApiResponse(responseCode = "409",
                    description = "Já existe outra tag com o mesmo nome. Resposta em JSON estruturado (StandardError).",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.juridiqsystem.crm.service.exceptions.StandardError.class)))
    })
    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@Parameter(description = "Id da tag", required = true) @PathVariable String id, @RequestBody @Valid TagRequestDTO tag) {
        tagService.update(id, tag);
        return ResponseEntity.ok().build();
    }




}

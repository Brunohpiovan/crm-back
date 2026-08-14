package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.CadenciaFunil;
import com.juridiqsystem.crm.model.Tag;
import com.juridiqsystem.crm.model.dtos.CadenciaAllDTO;
import com.juridiqsystem.crm.model.dtos.CadenciaFunilDto;
import com.juridiqsystem.crm.model.dtos.CadenciaFunilRequestDto;
import com.juridiqsystem.crm.service.CadenciaFunilService;
import com.juridiqsystem.crm.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Cadência de Funil", description = "Regras de movimentação automática de oportunidades entre etapas/funis com base no tempo de permanência em uma etapa (executadas por um scheduler em segundo plano). CRUD das cadências cadastradas.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/cadencia")
public class CadenciaFunilController {

    @Autowired
    private CadenciaFunilService cadenciaFunilService;

    @Operation(summary = "Listar todas as cadências de funil", description = "Requer JWT. Retorna todas as cadências cadastradas, com os nomes do funil/etapa de origem e destino já resolvidos.")
    @ApiResponse(responseCode = "200", description = "Lista de cadências",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CadenciaAllDTO.class))))
    @GetMapping
    public List<?> findAll() {
        return cadenciaFunilService.findAll();
    }

    @Operation(summary = "Buscar cadência de funil por id", description = "Requer JWT. Retorna os detalhes completos (incluindo ids de funil/etapa de origem e destino) de uma cadência.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadência encontrada",
                    content = @Content(schema = @Schema(implementation = CadenciaFunilDto.class))),
            @ApiResponse(responseCode = "400", description = "Cadência não encontrada para o id informado (resposta em JSON estruturado — StandardError)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Cadencia de Funil nao encontrada")))
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@Parameter(description = "Id da cadência de funil", required = true) @PathVariable String id){
        return ResponseEntity.ok(cadenciaFunilService.findById(id));
    }

    @Operation(summary = "Criar uma nova cadência de funil",
            description = "Requer JWT. Cadastra uma regra que, quando ativa (`situacao = ATIVA`), move automaticamente oportunidades da etapa/funil de origem para a etapa/funil de destino após permanecerem `diasNaEtapa` dias na etapa de origem, no horário `horarioMovimentacao` (execução por um scheduler que roda em segundo plano).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadência criada com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400", description = "Funil ou etapa de origem/destino informado não encontrado (resposta em JSON estruturado — StandardError; este endpoint não usa @Valid, então não há erro de validação em formato de mapa)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Funil de origem nao encontrado")))
    })
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid CadenciaFunilRequestDto cadenciaFunil) {
        cadenciaFunilService.create(cadenciaFunil);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Excluir uma cadência de funil", description = "Requer JWT. Remove a regra de cadência. Não afeta oportunidades já movidas anteriormente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadência apagada com sucesso",
                    content = @Content(schema = @Schema(type = "object", example = "{\"message\": \"Cadencia apagada com sucesso\"}"))),
            @ApiResponse(responseCode = "400", description = "Cadência não encontrada para o id informado (resposta em JSON estruturado — StandardError). Observação: a mensagem lançada pelo serviço, por um erro de copiar/colar no código-fonte, é \"Tag nao encontrada\" mesmo sendo uma cadência de funil.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Tag nao encontrada")))
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "Id da cadência de funil", required = true) @PathVariable String id) {
        cadenciaFunilService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Cadencia apagada com sucesso"));
    }

    @Operation(summary = "Atualizar uma cadência de funil",
            description = "Requer JWT. Substitui todos os dados da cadência (nome, funil/etapa de origem e destino, dias na etapa, horário de movimentação, situação e descrição). Corpo validado com Bean Validation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadência atualizada com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400",
                    description = "Erro de validação Bean Validation dos campos do corpo da requisição (mapa simples campo -> mensagem).",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"campo\": \"mensagem de erro\"}"))),
            @ApiResponse(responseCode = "404",
                    description = "Cadência, funil ou etapa de origem/destino não encontrado. O serviço lança ResponseStatusException(HttpStatus.NOT_FOUND, ...), tratado por um handler dedicado no ResourceExceptionHandler que preserva o status real e responde em JSON estruturado (StandardError).",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = com.juridiqsystem.crm.service.exceptions.StandardError.class)))
    })
    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@Parameter(description = "Id da cadência de funil", required = true) @PathVariable String id, @RequestBody @Valid CadenciaFunilRequestDto cadenciaFunil) {
        cadenciaFunilService.update(id, cadenciaFunil);
        return ResponseEntity.ok().build();
    }
}

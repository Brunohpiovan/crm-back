package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaCreateRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaFunilDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaUpdate2DTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EtapaUpdateDTO;
import br.edu.faculdadevincit.crm_vincit.service.EtapaService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Etapa", description = "Etapas (colunas) de um funil de vendas: CRUD e consulta por funil. Cada etapa acumula o valor total (soma) das oportunidades nela contidas.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/etapa")
public class EtapaController {


    @Autowired
    private EtapaService etapaService;

    @Operation(summary = "Listar todas as etapas do sistema", description = "Requer JWT. Retorna todas as etapas cadastradas (de todos os funis), cada uma já com suas oportunidades.")
    @ApiResponse(responseCode = "200", description = "Lista de etapas",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EtapaDto.class))))
    @GetMapping
    public List<EtapaDto> findAll() {
        return etapaService.findAll();
    }

    @Operation(summary = "Listar etapas de um funil", description = "Requer JWT. Retorna apenas id e nome das etapas pertencentes ao funil informado (sem as oportunidades), útil para seletores/dropdowns.")
    @ApiResponse(responseCode = "200", description = "Lista de etapas do funil (vazia se o funil não existir ou não tiver etapas)",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EtapaFunilDTO.class))))
    @GetMapping(value = "funil/{id}")
    public List<EtapaFunilDTO> findByFunilId(@Parameter(description = "Id do funil", required = true) @PathVariable Long id){
        return etapaService.findByFunilId(id);
    }

    @Operation(summary = "Buscar etapa por id", description = "Requer JWT. Retorna a etapa com suas oportunidades.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Etapa encontrada",
                    content = @Content(schema = @Schema(implementation = EtapaDto.class))),
            @ApiResponse(responseCode = "400", description = "Etapa não encontrada para o id informado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Etapa com id 1 não encontrado")))
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@Parameter(description = "Id da etapa", required = true) @PathVariable Long id) {
        EtapaDto etapa = etapaService.findById(id);
        return ResponseEntity.ok(etapa);
    }

    @Operation(summary = "Criar uma nova etapa", description = "Requer JWT. Cria uma etapa vinculada a um funil existente (id do funil informado dentro do corpo). Não é permitido criar duas etapas com o mesmo nome no mesmo funil. O valor total da etapa começa zerado. Após a criação, o backend publica a nova etapa no canal WebSocket /topic/newetapa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Etapa criada com sucesso",
                    content = @Content(schema = @Schema(implementation = EtapaDto.class))),
            @ApiResponse(responseCode = "400", description = "Já existe uma etapa com o mesmo nome neste funil, ou o funil informado não foi encontrado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Já existe um etapa com o mesmo nome neste funil.")))
    })
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid EtapaCreateRequest etapaCreateRequest) {
        EtapaDto newEtapa = etapaService.create(etapaCreateRequest);
        return ResponseEntity.ok(newEtapa);
    }

    @Operation(summary = "Atualizar uma etapa", description = "Requer JWT. Atualiza o nome (e, opcionalmente, o funil) de uma etapa existente. Não é permitido renomear para um nome já usado por outra etapa do mesmo funil. Após a atualização, o backend publica a etapa atualizada no canal WebSocket /topic/updateEtapa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Etapa atualizada com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400", description = "Etapa/funil não encontrado, ou já existe outra etapa com o mesmo nome neste funil (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Etapa com id 1 nao encontrada")))
    })
    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@Parameter(description = "Id da etapa", required = true) @PathVariable Long id,@RequestBody EtapaUpdateDTO etapaRequest) {
        etapaService.update(id, etapaRequest);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Excluir uma etapa", description = "Requer JWT. Remove a etapa e, em cascata, as oportunidades nela contidas. Após a exclusão, o backend publica o id da etapa removida no canal WebSocket /topic/deleteetapa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Etapa excluída com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400", description = "Etapa não encontrada para o id informado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Etapa com id 1 nao encontrada")))
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "Id da etapa", required = true) @PathVariable Long id) {
        etapaService.delete(id);
        return ResponseEntity.ok().build();
    }
}

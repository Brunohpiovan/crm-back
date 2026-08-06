package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.EquipeCreateRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EquipeResponse;
import br.edu.faculdadevincit.crm_vincit.service.EquipeService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Equipe", description = "Grupos simples de usuários (só nome + membros, sem hierarquia), usados para dar efeito ao filtro teamId do dashboard.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/equipe")
public class EquipeController {

    @Autowired
    private EquipeService equipeService;

    @Operation(summary = "Listar equipes", description = "Requer JWT. Retorna todas as equipes cadastradas com seus membros.")
    @ApiResponse(responseCode = "200", description = "Lista de equipes",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = EquipeResponse.class))))
    @GetMapping
    public List<EquipeResponse> findAll() {
        return equipeService.findAll();
    }

    @Operation(summary = "Criar uma equipe", description = "Requer JWT. Cria uma equipe vazia (sem membros) a partir do nome informado.")
    @ApiResponse(responseCode = "200", description = "Equipe criada com sucesso",
            content = @Content(schema = @Schema(implementation = EquipeResponse.class)))
    @PostMapping
    public ResponseEntity<EquipeResponse> create(@RequestBody @Valid EquipeCreateRequest request) {
        return ResponseEntity.ok(equipeService.create(request));
    }

    @Operation(summary = "Renomear uma equipe", description = "Requer JWT. Atualiza o nome de uma equipe existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipe atualizada com sucesso",
                    content = @Content(schema = @Schema(implementation = EquipeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada para o id informado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EquipeResponse> rename(
            @Parameter(description = "Id da equipe", required = true) @PathVariable String id,
            @RequestBody @Valid EquipeCreateRequest request) {
        return ResponseEntity.ok(equipeService.rename(id, request));
    }

    @Operation(summary = "Adicionar um membro à equipe",
            description = "Requer JWT. Associa o usuário informado à equipe informada. Operação idempotente: se o usuário já for membro, nada é alterado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membro adicionado com sucesso",
                    content = @Content(schema = @Schema(implementation = EquipeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Equipe ou usuário não encontrado")
    })
    @PostMapping("/add-membro/{equipeId}/{usuarioId}")
    public ResponseEntity<EquipeResponse> addMembro(
            @Parameter(description = "Id da equipe", required = true) @PathVariable String equipeId,
            @Parameter(description = "Id do usuário a ser adicionado", required = true) @PathVariable String usuarioId) {
        return ResponseEntity.ok(equipeService.addMembro(equipeId, usuarioId));
    }

    @Operation(summary = "Remover um membro da equipe",
            description = "Requer JWT. Remove a associação do usuário informado com a equipe informada. Operação idempotente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membro removido com sucesso",
                    content = @Content(schema = @Schema(implementation = EquipeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada")
    })
    @DeleteMapping("/remove-membro/{equipeId}/{usuarioId}")
    public ResponseEntity<EquipeResponse> removeMembro(
            @Parameter(description = "Id da equipe", required = true) @PathVariable String equipeId,
            @Parameter(description = "Id do usuário a ser removido", required = true) @PathVariable String usuarioId) {
        return ResponseEntity.ok(equipeService.removeMembro(equipeId, usuarioId));
    }

    @Operation(summary = "Excluir uma equipe", description = "Requer JWT. Remove a equipe e os vínculos com seus membros.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipe excluída com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "404", description = "Equipe não encontrada para o id informado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "Id da equipe", required = true) @PathVariable String id) {
        equipeService.delete(id);
        return ResponseEntity.ok().build();
    }
}

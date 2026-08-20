package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.CargoCreateRequest;
import com.juridiqsystem.crm.model.dtos.CargoResponse;
import com.juridiqsystem.crm.model.dtos.CargoUpdateRequest;
import com.juridiqsystem.crm.model.dtos.PermissaoResponse;
import com.juridiqsystem.crm.service.CargoService;
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

@Tag(name = "Cargo", description = "Cargos customizáveis da empresa e as permissões delegadas a cada um. Todas as rotas exigem ROLE_ADMIN: configurar permissões é admin-only e não é delegável por cargo.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/cargos")
public class CargoController {

    @Autowired
    private CargoService cargoService;

    @Operation(summary = "Listar os cargos da empresa",
            description = "Requer JWT de administrador. Inclui o cargo administrador (fixo), cujas permissões vêm vazias por definição — ele tem acesso total independente da lista.")
    @ApiResponse(responseCode = "200", description = "Cargos da empresa",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CargoResponse.class))))
    @GetMapping
    public List<CargoResponse> listar() {
        return cargoService.listar();
    }

    @Operation(summary = "Listar as permissões delegáveis do sistema",
            description = "Requer JWT de administrador. Catálogo fixo (enum Permissao) com rótulo amigável — usado pelo frontend para renderizar os checkboxes sem duplicar a lista.")
    @ApiResponse(responseCode = "200", description = "Permissões disponíveis",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PermissaoResponse.class))))
    @GetMapping("/permissoes-disponiveis")
    public List<PermissaoResponse> permissoesDisponiveis() {
        return cargoService.permissoesDisponiveis();
    }

    @Operation(summary = "Criar um cargo customizado",
            description = "Requer JWT de administrador. O cargo criado nunca é administrador: esse é fixo, único por empresa e criado junto com ela.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cargo criado",
                    content = @Content(schema = @Schema(implementation = CargoResponse.class))),
            @ApiResponse(responseCode = "409", description = "Já existe um cargo com este nome na empresa")
    })
    @PostMapping
    public ResponseEntity<CargoResponse> criar(@RequestBody @Valid CargoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cargoService.criar(request));
    }

    @Operation(summary = "Atualizar nome e permissões de um cargo",
            description = "Requer JWT de administrador. As permissões enviadas substituem integralmente as anteriores. O cargo administrador é rejeitado (403).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cargo atualizado",
                    content = @Content(schema = @Schema(implementation = CargoResponse.class))),
            @ApiResponse(responseCode = "403", description = "Tentativa de alterar o cargo administrador (fixo do sistema)"),
            @ApiResponse(responseCode = "404", description = "Cargo não encontrado nesta empresa"),
            @ApiResponse(responseCode = "409", description = "Já existe um cargo com este nome na empresa")
    })
    @PutMapping("/{publicId}")
    public CargoResponse atualizar(
            @Parameter(description = "Id público do cargo", required = true) @PathVariable String publicId,
            @RequestBody @Valid CargoUpdateRequest request) {
        return cargoService.atualizar(publicId, request);
    }

    @Operation(summary = "Excluir um cargo customizado",
            description = "Requer JWT de administrador. Rejeita o cargo administrador (403) e cargos que ainda tenham usuários vinculados (409).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cargo excluído"),
            @ApiResponse(responseCode = "403", description = "Tentativa de excluir o cargo administrador (fixo do sistema)"),
            @ApiResponse(responseCode = "404", description = "Cargo não encontrado nesta empresa"),
            @ApiResponse(responseCode = "409", description = "Cargo ainda vinculado a usuários")
    })
    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> excluir(
            @Parameter(description = "Id público do cargo", required = true) @PathVariable String publicId) {
        cargoService.excluir(publicId);
        return ResponseEntity.noContent().build();
    }
}

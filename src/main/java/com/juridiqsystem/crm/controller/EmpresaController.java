package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.EmpresaCreateDTO;
import com.juridiqsystem.crm.model.dtos.EmpresaResponseDTO;
import com.juridiqsystem.crm.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Master - Empresas", description = "CRUD de empresas (tenants), restrito ao usuário master (ROLE_MASTER). Não inclui a empresa interna do próprio master.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/master/empresas")
public class EmpresaController {

    @Autowired
    private EmpresaService empresaService;

    @Operation(summary = "Listar empresas (paginado, com busca opcional)", description = "Requer JWT com cargo ROLE_MASTER. `search` filtra por nome/código (case-insensitive); se omitido, retorna todas as empresas não-internas. Cada item inclui a quantidade de usuários ativos e de administradores da empresa.")
    @ApiResponse(responseCode = "200", description = "Página de empresas")
    @GetMapping
    public Page<EmpresaResponseDTO> findAll(
            @Parameter(description = "Termo de busca opcional (nome/código)") @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return empresaService.findAll(search, pageable);
    }

    @Operation(summary = "Buscar empresa por id", description = "Requer JWT com cargo ROLE_MASTER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa encontrada"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para o id informado")
    })
    @GetMapping("/{id}")
    public EmpresaResponseDTO findById(@Parameter(description = "Id da empresa", required = true) @PathVariable String id) {
        return empresaService.findById(id);
    }

    @Operation(summary = "Criar uma empresa", description = "Requer JWT com cargo ROLE_MASTER. `codigo` deve ser único no sistema (usado no login).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Código já usado por outra empresa")
    })
    @PostMapping
    public EmpresaResponseDTO create(@RequestBody @Valid EmpresaCreateDTO dto) {
        return empresaService.create(dto);
    }

    @Operation(summary = "Atualizar uma empresa", description = "Requer JWT com cargo ROLE_MASTER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empresa atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Código já usado por outra empresa"),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada para o id informado")
    })
    @PutMapping("/{id}")
    public EmpresaResponseDTO update(@Parameter(description = "Id da empresa", required = true) @PathVariable String id,
                                      @RequestBody @Valid EmpresaCreateDTO dto) {
        return empresaService.update(id, dto);
    }
}

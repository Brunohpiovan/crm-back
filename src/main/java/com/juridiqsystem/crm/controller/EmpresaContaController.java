package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.EmpresaResponseDTO;
import com.juridiqsystem.crm.model.dtos.EmpresaSelfUpdateDTO;
import com.juridiqsystem.crm.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Empresa (autoatendimento)", description = "Dados da empresa do usuário autenticado (aba Configurações do CRM). Diferente de /master/empresas/**, que é o CRUD do usuário master sobre qualquer empresa-cliente.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/empresa")
public class EmpresaContaController {

    @Autowired
    private EmpresaService empresaService;

    @Operation(summary = "Buscar dados da própria empresa", description = "Requer JWT com cargo ROLE_VENDEDOR (qualquer usuário autenticado do CRM). Resolve a empresa a partir do token, sem receber id.")
    @ApiResponse(responseCode = "200", description = "Empresa do usuário autenticado")
    @GetMapping
    public EmpresaResponseDTO buscar() {
        return empresaService.getEmpresaAutenticada();
    }

    @Operation(
            summary = "Atualizar dados da própria empresa",
            description = """
                    Requer JWT com cargo ROLE_ADMIN. Recebe multipart/form-data com a parte \
                    `empresa` (JSON com EmpresaSelfUpdateDTO) e opcionalmente a parte `logo` \
                    (arquivo de imagem; se ausente, mantém/usa o `logoUrl` informado no JSON, \
                    permitindo também remover o logo enviando `logoUrl` vazio). Não permite \
                    alterar `codigo` (login da empresa).
                    """
    )
    @ApiResponse(responseCode = "200", description = "Empresa atualizada com sucesso")
    @PutMapping
    public EmpresaResponseDTO atualizar(@Parameter(description = "Dados atualizados da empresa", required = true) @RequestPart("empresa") @Valid EmpresaSelfUpdateDTO dto,
                                         @Parameter(description = "Novo logo (opcional)") @RequestPart(value = "logo", required = false) MultipartFile logo) {
        return empresaService.atualizarEmpresaAutenticada(dto, logo);
    }
}

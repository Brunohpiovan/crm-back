package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.*;
import com.juridiqsystem.crm.service.CargoService;
import com.juridiqsystem.crm.service.EmpresaService;
import com.juridiqsystem.crm.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Master - Usuários", description = "CRUD de usuários de uma empresa-cliente específica, restrito ao usuário master (ROLE_MASTER). Espelha as regras de UsuarioController, mas mirando a empresa informada em `empresaId` em vez da empresa do próprio chamador.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/master/empresas/{empresaId}/usuarios")
public class MasterUsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmpresaService empresaService;

    @Autowired
    private CargoService cargoService;

    @Operation(summary = "Listar usuários de uma empresa (paginado, com busca opcional)", description = "Requer JWT com cargo ROLE_MASTER.")
    @GetMapping
    public Page<UsuarioAllDTO> findAll(
            @Parameter(description = "Id da empresa", required = true) @PathVariable String empresaId,
            @Parameter(description = "Termo de busca opcional (nome/login)") @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return usuarioService.findAllParaEmpresa(empresaService.resolverIdInterno(empresaId), search, pageable);
    }

    @Operation(
            summary = "Listar os cargos da empresa, para atribuir a um usuário",
            description = """
                    Requer JWT com cargo ROLE_MASTER. O master pertence à empresa interna, então                     GET /cargos (que resolve a empresa pelo próprio usuário logado) não serviria                     aqui — este endpoint mira a empresa-cliente informada na URL.
                    """
    )
    @GetMapping("/cargos-disponiveis")
    public List<CargoResponse> cargosDisponiveis(
            @Parameter(description = "Id da empresa", required = true) @PathVariable String empresaId) {
        return cargoService.listarParaEmpresa(empresaService.resolverIdInterno(empresaId));
    }

    @Operation(summary = "Buscar usuário por id, para edição", description = "Requer JWT com cargo ROLE_MASTER.")
    @GetMapping("/{id}/edicao")
    public UsuarioResponseNoAuthDto findByIdParaEdicao(
            @Parameter(description = "Id da empresa", required = true) @PathVariable String empresaId,
            @Parameter(description = "Id do usuário", required = true) @PathVariable String id) {
        return usuarioService.findByIdParaEdicaoMaster(empresaService.resolverIdInterno(empresaId), id);
    }

    @Operation(
            summary = "Criar novo usuário numa empresa",
            description = """
                    Requer JWT com cargo ROLE_MASTER. Recebe multipart/form-data com a parte \
                    `usuario` (JSON com os dados do UsuarioCreateDTO) e opcionalmente a parte \
                    `foto`. Ao criar o usuário, um Participante espelhado (tipo FUNCIONARIO) \
                    também é criado automaticamente na mesma empresa, para permitir chat \
                    interno com esse usuário — mesmo comportamento de POST /usuario.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuário criado com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Login (e-mail) ou CPF já cadastrado nesta empresa")
    })
    @PostMapping
    public ResponseEntity<?> post(
            @Parameter(description = "Id da empresa", required = true) @PathVariable String empresaId,
            @Parameter(description = "Dados do usuário a ser criado", required = true) @RequestPart("usuario") @Valid UsuarioCreateDTO usuarioRequest,
            @Parameter(description = "Foto/avatar do usuário (opcional)") @RequestPart(value = "foto", required = false) MultipartFile foto) {
        UsuarioAllDTO dto = usuarioService.saveParaEmpresa(empresaService.resolverIdInterno(empresaId), usuarioRequest, foto);
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "Atualizar usuário (inclui cargo e bloqueado)",
            description = "Requer JWT com cargo ROLE_MASTER. Recebe multipart/form-data com a parte `usuario` (UsuarioAdminUpdateDTO) e opcionalmente `foto`."
    )
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @Parameter(description = "Id da empresa", required = true) @PathVariable String empresaId,
            @Parameter(description = "Id do usuário", required = true) @PathVariable String id,
            @Parameter(description = "Dados administrativos atualizados", required = true) @RequestPart("usuario") @Valid UsuarioAdminUpdateDTO usuarioRequest,
            @Parameter(description = "Nova foto/avatar (opcional)") @RequestPart(value = "foto", required = false) MultipartFile foto) {
        UsuarioAllDTO responseDTO = usuarioService.updateAllParaEmpresa(empresaService.resolverIdInterno(empresaId), id, usuarioRequest, foto);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(
            summary = "Excluir (inativar) usuário",
            description = "Requer JWT com cargo ROLE_MASTER. Assim como DELETE /usuario/{id}, não é uma exclusão física: apenas marca `bloqueado = true`."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @Parameter(description = "Id da empresa", required = true) @PathVariable String empresaId,
            @Parameter(description = "Id do usuário", required = true) @PathVariable String id) {
        usuarioService.deleteParaEmpresa(empresaService.resolverIdInterno(empresaId), id);
        return ResponseEntity.ok().build();
    }
}

package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.*;
import com.juridiqsystem.crm.service.UsuarioService;
import com.juridiqsystem.crm.service.exceptions.StandardError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

/*
 * NOTA sobre a anotação de respostas de erro neste arquivo: como ele importa
 * com.juridiqsystem.crm.model.dtos.* (que inclui o DTO ApiResponse,
 * usado pelo ResourceExceptionHandler), a anotação do springdoc é referenciada
 * de forma totalmente qualificada (@io.swagger.v3.oas.annotations.responses.ApiResponse)
 * para evitar colisão de nomes com esse DTO.
 */
@Tag(name = "Usuário", description = "CRUD de usuários do CRM (funcionários/atendentes). Regras de cargo variam por endpoint: consulte a description de cada operação.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Operation(
            summary = "Listar usuários (paginado, com busca opcional)",
            description = """
                    Requer JWT com cargo ROLE_VENDEDOR (usuários ADMINISTRADOR também possuem \
                    ROLE_VENDEDOR, então todo usuário autenticado do CRM consegue acessar). \
                    `search` filtra por nome/login (case-insensitive); se omitido ou em branco, \
                    retorna todos os usuários, incluindo os bloqueados (necessário para que um \
                    administrador consiga localizar e desbloquear um usuário). Resultado paginado \
                    (parâmetros padrão do Spring Data: `page`, `size`, `sort`), ordenado por `nome` \
                    por padrão.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Página de usuários resumidos")
    @GetMapping
    public Page<UsuarioAllDTO> findAll(
            @Parameter(description = "Termo de busca opcional (nome/login)") @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return usuarioService.findAll(search, pageable);
    }

    @Operation(summary = "Listar contatos do usuário (para chat interno)", description = "Requer JWT com cargo ROLE_VENDEDOR. Retorna os usuários com quem `userId` compartilha algum grupo privado de chat interno.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de contatos", content = @Content(array = @ArraySchema(schema = @Schema(implementation = UsuarioAllContactsDTO.class))))
    @GetMapping("/contacts/{userId}")
    public List<UsuarioAllContactsDTO> findAllContacts(@Parameter(description = "Id do usuário", required = true) @PathVariable String userId) {
        return usuarioService.findAllContacts(userId);
    }

    @Operation(summary = "Listar usuários disponíveis para novo grupo/contato", description = "Requer JWT com cargo ROLE_VENDEDOR. Retorna os usuários que ainda não compartilham nenhum grupo privado de chat interno com `userId`.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de usuários disponíveis", content = @Content(array = @ArraySchema(schema = @Schema(implementation = UsuarioAllContactsDTO.class))))
    @GetMapping("/contacts/dispo/{userId}")
    public List<UsuarioAllContactsDTO> findAllContactsDispo(@Parameter(description = "Id do usuário", required = true) @PathVariable String userId) {
        return usuarioService.findAllContactsDispo(userId);
    }

    @Operation(summary = "Listar possíveis criadores", description = "Requer JWT com cargo ROLE_VENDEDOR. Retorna um resumo (id, nome, email, celular, foto) de todos os usuários, usado para seleção de \"criador\" em outras telas (ex.: funil, oportunidade).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de criadores", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CriadorDto.class))))
    @GetMapping("/criador")
    public List<CriadorDto> findAllcriador() {
        return usuarioService.findAllCriador();
    }

    @Operation(summary = "Listar usuários administradores", description = "Requer JWT com cargo ROLE_VENDEDOR. Retorna o resumo (id, nome, login, celular) de todos os usuários com cargo ADMINISTRADOR.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de administradores", content = @Content(array = @ArraySchema(schema = @Schema(implementation = UsuarioAllDTO.class))))
    @GetMapping("/admin")
    public List<UsuarioAllDTO> findCargo() {
        return usuarioService.findByAdmin();
    }

    @Operation(
            summary = "Buscar usuário por id, para edição",
            description = """
                    Requer JWT com cargo ROLE_VENDEDOR. Retorna os dados completos do usuário \
                    identificado por `id`, incluindo `cargo` (enum) e `bloqueado`, para \
                    pré-preencher o formulário de edição. Assim como em GET /usuario/{id}, o \
                    service valida que o solicitante seja ADMINISTRADOR ou o próprio dono do \
                    cadastro (comparando login do token); caso contrário, 403.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuário encontrado", content = @Content(schema = @Schema(implementation = UsuarioResponseNoAuthDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Usuário não encontrado para o id informado. Resposta em JSON estruturado (StandardError) — cai no handler genérico de RuntimeException.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "O usuário autenticado não é ADMINISTRADOR nem o dono do cadastro solicitado",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping(value = "/{id}/edicao")
    public ResponseEntity<?> findByIdParaEdicao(@Parameter(description = "Id do usuário", required = true) @PathVariable String id) {
        UsuarioResponseNoAuthDto resposta = usuarioService.findByIdParaEdicao(id);
        return ResponseEntity.ok(resposta);
    }

    @Operation(
            summary = "Buscar usuário por id",
            description = """
                    Requer JWT com cargo ROLE_VENDEDOR. Além da exigência de cargo, o service \
                    valida que o solicitante seja ADMINISTRADOR ou o próprio dono do cadastro \
                    (comparando o login do token com o login do usuário buscado); caso \
                    contrário, retorna 403 (AccessDeniedException).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuário encontrado", content = @Content(schema = @Schema(implementation = UsuarioResponseDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Usuário não encontrado para o id informado. Resposta em JSON estruturado (StandardError) — cai no handler genérico de RuntimeException.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "O usuário autenticado não é ADMINISTRADOR nem o dono do cadastro solicitado",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@Parameter(description = "Id do usuário", required = true) @PathVariable String id) {
        UsuarioResponseDto resposta = usuarioService.findById(id);
        return ResponseEntity.ok(resposta);
    }

    @Operation(
            summary = "Criar novo usuário",
            description = """
                    Requer JWT com cargo ROLE_ADMIN. Recebe multipart/form-data com a parte \
                    `usuario` (JSON com os dados do UsuarioCreateDTO) e opcionalmente a parte \
                    `foto` (arquivo de imagem do avatar; se ausente, é usado um avatar padrão). \
                    Ao criar o usuário, um Participante espelhado (tipo FUNCIONARIO) também é \
                    criado automaticamente, para permitir chat interno com esse usuário.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuário criado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioAllDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Login (e-mail) ou CPF já cadastrado no sistema. Nota: os endpoints deste controller não usam @Valid, então as anotações Bean Validation dos DTOs (@NotNull/@Size/@Email) não são de fato disparadas pelo Spring; o único 400 real de validação de negócio é esta duplicidade.",
                    content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    @PostMapping
    public ResponseEntity<?> post(@Parameter(description = "Dados do usuário a ser criado", required = true) @RequestPart("usuario") @Valid UsuarioCreateDTO usuarioRequest,
                                        @Parameter(description = "Foto/avatar do usuário (opcional)") @RequestPart(value = "foto", required = false) MultipartFile foto) {
        UsuarioAllDTO dto = usuarioService.save(usuarioRequest,foto);
        return ResponseEntity.ok(dto);

    }

    @Operation(
            summary = "Atualizar o próprio cadastro",
            description = """
                    Sem regra de cargo específica: exige apenas um usuário autenticado (qualquer \
                    cargo), diferente dos demais endpoints de escrita deste controller que \
                    exigem ROLE_ADMIN. O service ainda valida que `id` corresponde ao próprio \
                    usuário do token (senão 403). Não permite alterar `cargo` nem `bloqueado` \
                    (use PUT /usuario/all/{id} para isso). Recebe multipart/form-data com a \
                    parte `usuario` (UsuarioSelfUpdateDTO) e opcionalmente `foto`. Retorna um \
                    novo token JWT (o login pode ter mudado).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuário atualizado; retorna novo token JWT", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Duas causas possíveis, ambas em JSON estruturado (StandardError): (1) usuário não encontrado ou senhas não coincidem (RuntimeException/ResponseStatusException genéricos); (2) login/CPF já cadastrado em outro usuário (DataIntegrityViolationException dedicada)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "O usuário autenticado não é o dono do cadastro informado",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@Parameter(description = "Id do usuário", required = true) @PathVariable String id,
                                    @Parameter(description = "Dados pessoais atualizados", required = true) @RequestPart("usuario") @Valid UsuarioSelfUpdateDTO usuarioRequest,
                                    @Parameter(description = "Nova foto/avatar (opcional)") @RequestPart(value = "foto", required = false) MultipartFile foto) {
        LoginResponseDTO responseDTO = usuarioService.update(id, usuarioRequest, foto);
        return ResponseEntity.ok(responseDTO);

    }

    @Operation(
            summary = "Atualizar usuário (administrativo, inclui cargo e bloqueado)",
            description = """
                    Requer JWT com cargo ROLE_ADMIN. É o único endpoint de atualização que pode \
                    alterar `cargo` e `bloqueado` de qualquer usuário. Recebe multipart/form-data \
                    com a parte `usuario` (UsuarioAdminUpdateDTO) e opcionalmente `foto`.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuário atualizado", content = @Content(schema = @Schema(implementation = UsuarioAllDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Duas causas possíveis, ambas em JSON estruturado (StandardError): (1) usuário não encontrado (UserNotFoundException, cai no handler genérico de RuntimeException); (2) login/CPF já cadastrado em outro usuário (DataIntegrityViolationException dedicada)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class)))
    })
    @PutMapping(value = "/all/{id}")
    public ResponseEntity<?> updateAll(@Parameter(description = "Id do usuário", required = true) @PathVariable String id,
                                    @Parameter(description = "Dados administrativos atualizados (inclui cargo e bloqueado)", required = true) @RequestPart("usuario") @Valid UsuarioAdminUpdateDTO usuarioRequest,
                                    @Parameter(description = "Nova foto/avatar (opcional)") @RequestPart(value = "foto", required = false) MultipartFile foto) {
        UsuarioAllDTO responseDTO = usuarioService.updateAll(id, usuarioRequest, foto);
        return ResponseEntity.ok(responseDTO);

    }

    @Operation(
            summary = "Excluir (inativar) usuário",
            description = """
                    Requer JWT com cargo ROLE_ADMIN. Não é uma exclusão física (hard delete): \
                    como o registro possui FKs restritivas (protocolo, e-mail, oportunidade, \
                    mensagem interna, log de acesso, etc.), a operação apenas marca `bloqueado = true`, \
                    preservando o histórico e impedindo login futuro desse usuário.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuário inativado (bloqueado) com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Usuário não encontrado para o id informado. Resposta em JSON estruturado (StandardError) — UserNotFoundException cai no handler genérico de RuntimeException.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class)))
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "Id do usuário", required = true) @PathVariable String id) {
        usuarioService.delete(id);
        return ResponseEntity.ok().build();
    }
}
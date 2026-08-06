package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.ProtocoloDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ProtocoloMoveDTO;
import br.edu.faculdadevincit.crm_vincit.service.ProtocoloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Tag(name = "Protocolo", description = """
        Protocolos de atendimento (vinculam um administrador/atendente a um participante enquanto \
        o atendimento está em aberto). Ações aqui disparam eventos WebSocket para o frontend: \
        criação publica em /topic/protocolo/aberto/{participanteId} e /topic/protocolo/novo; \
        encerramento publica em /topic/protocolo/{id} e /topic/contatoRet; encaminhamento publica \
        em /topic/protocolo/aberto/{adminId}.
        """)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/protocolos")
public class ProtocoloController {
    @Autowired
    private ProtocoloService protocoloService;

    @Operation(
            summary = "Abrir novo protocolo",
            description = """
                    Requer JWT. Cria um protocolo ABERTO vinculando o administrador (`id_admin`) \
                    ao participante (`id_participante`); se o participante informado não existir \
                    ainda como Participante, um novo é criado a partir do Usuario correspondente. \
                    Mensagens do participante que ainda não tinham protocolo são automaticamente \
                    vinculadas a este novo protocolo. Publica eventos WebSocket em \
                    /topic/protocolo/aberto/{participanteId} e /topic/protocolo/novo.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Protocolo criado", content = @Content(schema = @Schema(implementation = ProtocoloMoveDTO.class))),
            @ApiResponse(responseCode = "400", description = "Administrador não encontrado ou já existe protocolo em andamento com esse participante")
    })
    @PostMapping
    public ResponseEntity<?> createProtocol(@Parameter(description = "Dados do protocolo a ser criado (id_admin e id_participante são obrigatórios)", required = true) @RequestBody ProtocoloDto protocoloDto) {
        ProtocoloMoveDTO protocolo = protocoloService.createProtocolo(protocoloDto);
        return ResponseEntity.ok(protocolo);
    }

    @Operation(
            summary = "Encerrar (fechar) protocolo",
            description = """
                    Requer JWT. Marca o protocolo `id` como FECHADO e registra a data de \
                    encerramento. Publica eventos WebSocket em /topic/protocolo/{id} (novo status) \
                    e /topic/contatoRet (dados do participante).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Protocolo fechado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Admin autenticado não encontrado, protocolo não encontrado ou protocolo já encerrado")
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> closeProtocol(@Parameter(description = "Id do protocolo", required = true) @PathVariable String id) {
        protocoloService.closeProtocolo(id);
        return ResponseEntity.ok(Map.of("message", "Protocolo fechado com sucesso"));
    }

    @Operation(summary = "Buscar protocolo aberto entre dois usuários", description = "Requer JWT. `idUsuario1` é o id de um Usuario (administrador) e `idUsuario2` é o id de um Participante; retorna o protocolo ABERTO entre ambos (comparando celular), se existir.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Protocolo aberto encontrado", content = @Content(schema = @Schema(implementation = ProtocoloMoveDTO.class))),
            @ApiResponse(responseCode = "204", description = "Não há protocolo aberto entre os dois"),
            @ApiResponse(responseCode = "400", description = "Usuário ou participante não encontrado")
    })
    @GetMapping("/{idUsuario1}/{idUsuario2}")
    public ResponseEntity<?> getProtocoloByUsuario(@Parameter(description = "Id do usuário (administrador)", required = true) @PathVariable String idUsuario1, @Parameter(description = "Id do participante", required = true) @PathVariable String idUsuario2) {
        return protocoloService.getProtocoloByUsuario(idUsuario1, idUsuario2)
                .map(protocolo -> ResponseEntity.ok(new ProtocoloMoveDTO(protocolo)))
                .orElse(ResponseEntity.noContent().build());
    }



    @Operation(summary = "Listar protocolos do usuário autenticado", description = "Requer JWT. Retorna todos os protocolos (abertos e fechados) em que `idUsuario` é o administrador ou o participante. O service também valida que `idUsuario` corresponde ao login do token autenticado (senão 403).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de protocolos (pode ser vazia/null se o usuário não tiver protocolos)", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProtocoloMoveDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "O usuário autenticado não corresponde ao idUsuario informado")
    })
    @GetMapping("/get/{idUsuario}")
    public ResponseEntity<?> getProtocolo(@Parameter(description = "Id do usuário", required = true) @PathVariable String idUsuario){
        return ResponseEntity.ok(protocoloService.getProtocols(idUsuario));
    }

    @Operation(
            summary = "Listar protocolos do usuário autenticado (paginado)",
            description = """
                    Requer JWT. Mesma checagem de autorização de GET /protocolos/get/{idUsuario} \
                    (idUsuario precisa corresponder ao login do token autenticado), porém com \
                    paginação (page/size) e busca opcional por id, nome do administrador ou nome \
                    do participante. Ordenado por id decrescente (mais recentes primeiro) por padrão.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de protocolos do usuário"),
            @ApiResponse(responseCode = "400", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "403", description = "O usuário autenticado não corresponde ao idUsuario informado")
    })
    @GetMapping("/get/{idUsuario}/paginado")
    public Page<ProtocoloMoveDTO> getProtocolosPaginado(
            @Parameter(description = "Id do usuário", required = true) @PathVariable String idUsuario,
            @Parameter(description = "Termo de busca opcional (id, nome do administrador ou do participante)") @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 6, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return protocoloService.getProtocolsPaginado(idUsuario, search, pageable);
    }

    @Operation(summary = "Buscar protocolo por id", description = "Requer JWT. Retorna o protocolo `id` apenas se o usuário autenticado for o administrador ou o participante desse protocolo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Protocolo encontrado", content = @Content(schema = @Schema(implementation = ProtocoloMoveDTO.class))),
            @ApiResponse(responseCode = "400", description = "Protocolo não encontrado ou usuário autenticado não autorizado a acessá-lo")
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@Parameter(description = "Id do protocolo", required = true) @PathVariable String id){
        return ResponseEntity.ok(protocoloService.findById(id));
    }

    @Operation(
            summary = "Encaminhar protocolo para outro administrador",
            description = """
                    Requer JWT. Transfere o protocolo `id_Protocolo` para o novo administrador \
                    `id_admin`, preservando o administrador anterior (`adminAnterior`). Publica \
                    evento WebSocket em /topic/protocolo/aberto/{novoAdminId}.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Protocolo encaminhado", content = @Content(schema = @Schema(implementation = ProtocoloMoveDTO.class))),
            @ApiResponse(responseCode = "400", description = "Protocolo ou administrador não encontrado, ou já existe protocolo aberto para o novo administrador com esse participante")
    })
    @PutMapping("/encaminhar")
    public ResponseEntity<ProtocoloMoveDTO> encaminharProtocolo(@Parameter(description = "Id do novo administrador/atendente", required = true) @RequestParam String id_admin, @Parameter(description = "Id do protocolo a ser encaminhado", required = true) @RequestParam String id_Protocolo) {
        ProtocoloMoveDTO protocolo = protocoloService.encaminha(id_admin, id_Protocolo);
            return new ResponseEntity<>(protocolo, HttpStatus.OK);
    }


}

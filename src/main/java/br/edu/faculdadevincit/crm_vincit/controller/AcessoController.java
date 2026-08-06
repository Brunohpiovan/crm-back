package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.AcessoResponseDto;
import br.edu.faculdadevincit.crm_vincit.service.AcessoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Log de Acesso", description = "Histórico de acessos (login/logout, IP, dispositivo, localização) dos usuários.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/log")
public class AcessoController {

    @Autowired
    private AcessoService acessoService;

    /*
    @PostMapping("/acesso")
    public ResponseEntity<Long> logAccess(@RequestBody LogAcessoDTO log) {
        Long logId = acessoService.save(log);
        return ResponseEntity.status(HttpStatus.CREATED).body(logId);
    }
    */


    @Operation(
            summary = "Finalizar sessão (registrar horário de saída)",
            description = """
                    Endpoint público (não exige JWT — é chamado no momento do logout, quando o \
                    token pode já ter sido descartado pelo frontend). Registra a data/hora de \
                    saída (`data_saida`) do log de acesso identificado por `id` (o `logId` \
                    retornado por `POST /auth/login`).
                    """
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horário de saída registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Log de acesso não encontrado para o id informado. Resposta em **texto puro** (não é um JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Log não encontrado")))
    })
    @PutMapping("/finalizar/{id}")
    public ResponseEntity<Void> updateLogExit(@Parameter(description = "Id do log de acesso (retornado como logId no login)", required = true) @PathVariable String id) {
        acessoService.updateExitTime(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Listar todos os logs de acesso do sistema", description = "Requer JWT. Retorna o histórico completo de acessos de todos os usuários.")
    @ApiResponse(responseCode = "200", description = "Lista de logs de acesso",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AcessoResponseDto.class))))
    @GetMapping
    public List<?> findAll() {
        return acessoService.findAllLogAcessos();
    }

    @Operation(summary = "Listar logs de acesso de um usuário", description = "Requer JWT. Retorna o histórico de acessos do usuário identificado por `id`.")
    @ApiResponse(responseCode = "200", description = "Lista de logs de acesso do usuário",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AcessoResponseDto.class))))
    @GetMapping("/{id}")
    public List<AcessoResponseDto> findAllByUser(@Parameter(description = "Id do usuário", required = true) @PathVariable String id) {
        return acessoService.findAllByUser(id);
    }

}

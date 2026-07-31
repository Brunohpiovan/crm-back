package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ParticipanteDTO;
import br.edu.faculdadevincit.crm_vincit.service.ParticipanteService;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.StandardError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Participante", description = "Participantes/contatos externos do CRM (clientes atendidos via WhatsApp, etc.) e usuários internos espelhados como participante para chat interno.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/participante")
public class ParticipanteController {
    @Autowired
    ParticipanteService participanteService;


    @Operation(summary = "Listar todos os participantes", description = "Requer JWT. Retorna todos os participantes cadastrados (funcionários e contatos externos).")
    @ApiResponse(responseCode = "200", description = "Lista de participantes", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ParticipanteDTO.class))))
    @GetMapping
    public List<ParticipanteDTO> findAll() {
        return participanteService.findAll();
    }

    @Operation(summary = "Listar participantes sem protocolo aberto com outro administrador", description = "Requer JWT. Retorna os participantes disponíveis para abertura de um novo protocolo com o admin `id` (exclui participantes que já possuem protocolo ABERTO com outro administrador).")
    @ApiResponse(responseCode = "200", description = "Lista filtrada de participantes", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ParticipanteDTO.class))))
    @GetMapping(value = "/filter/{id}")
    public List<ParticipanteDTO> findAllFilter(@Parameter(description = "Id do administrador de referência", required = true) @PathVariable Long id) {

        return participanteService.findAllFilter(id);
    }

    @Operation(summary = "Buscar participante por id", description = "Requer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante encontrado", content = @Content(schema = @Schema(implementation = ParticipanteDTO.class))),
            @ApiResponse(responseCode = "400", description = "Participante não encontrado para o id informado (resposta em texto puro, não JSON estruturado — UsernameNotFoundException sem handler dedicado, cai no handler genérico de RuntimeException)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Participante não encontrado")))
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@Parameter(description = "Id do participante", required = true) @PathVariable Long id) {
        ParticipanteDTO resposta = participanteService.findById(id);
        return ResponseEntity.ok(resposta);
    }

    @Operation(summary = "Criar participante", description = "Requer JWT. Cria um novo participante; o login é normalizado para minúsculas e a foto/avatar recebe o valor padrão do sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Violação de constraint única (login ou CPF já cadastrado) no banco de dados — JSON StandardError, produzido pelo handler de org.springframework.dao.DataIntegrityViolationException. Nota: este endpoint não usa @Valid, então as anotações Bean Validation da entidade Participante não são disparadas.",
                    content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    @PostMapping
    public ResponseEntity<?> post(@Parameter(description = "Dados do participante a ser criado", required = true) @RequestBody Participante participante) {
        participanteService.create(participante);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Atualizar participante", description = "Requer JWT. Atualiza os dados cadastrais do participante identificado por `id`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante atualizado", content = @Content(schema = @Schema(implementation = Participante.class))),
            @ApiResponse(responseCode = "400", description = "Duas formas possíveis: (1) participante não encontrado — texto puro (UsernameNotFoundException, sem handler dedicado); (2) login/CPF em conflito com outro registro — JSON StandardError (org.springframework.dao.DataIntegrityViolationException)",
                    content = {
                            @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Participante não encontrado")),
                            @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class))
                    })
    })
    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@Parameter(description = "Id do participante", required = true) @PathVariable Long id, @Parameter(description = "Dados atualizados do participante", required = true) @RequestBody Participante participante ) {
        Participante participanteResposta = participanteService.update(participante,id);
        return ResponseEntity.ok(participanteResposta);
    }

    @Operation(summary = "Excluir participante", description = "Requer JWT. Exclusão física (hard delete) do participante identificado por `id`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Participante não encontrado para o id informado (resposta em texto puro, não JSON estruturado — UsernameNotFoundException sem handler dedicado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Participante não encontrado")))
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "Id do participante", required = true) @PathVariable Long id) {
        participanteService.delete(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Buscar participante por celular", description = "Requer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante encontrado", content = @Content(schema = @Schema(implementation = Participante.class))),
            @ApiResponse(responseCode = "400", description = "Nenhum participante cadastrado com esse celular (resposta em texto puro, não JSON estruturado — RuntimeException genérica sem handler dedicado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Nao existe participante com esse celular cadastrado")))
    })
    @GetMapping(value = "/celular/{celular}")
    public ResponseEntity<?> findByCelular(@Parameter(description = "Número de celular do participante", required = true) @PathVariable String celular) {
        return ResponseEntity.ok(participanteService.findByCelular(celular));
    }

    @Operation(summary = "Buscar participante por login", description = "Requer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante encontrado", content = @Content(schema = @Schema(implementation = ParticipanteDTO.class))),
            @ApiResponse(responseCode = "400", description = "Nenhum participante cadastrado com esse login (resposta em texto puro, não JSON estruturado — RuntimeException genérica sem handler dedicado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Nao existe participante com esse login cadastrado")))
    })
    @GetMapping(value = "/login/{login}")
    public ResponseEntity<?> findByLogin(@Parameter(description = "Login (e-mail) do participante", required = true) @PathVariable String login) {
        return ResponseEntity.ok(participanteService.findByLogin(login));
    }


}

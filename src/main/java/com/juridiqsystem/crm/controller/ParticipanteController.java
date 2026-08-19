package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.Participante;
import com.juridiqsystem.crm.model.dtos.ParticipanteCreateRequest;
import com.juridiqsystem.crm.model.dtos.ParticipanteDTO;
import com.juridiqsystem.crm.model.dtos.ParticipanteUpdateRequest;
import com.juridiqsystem.crm.service.ParticipanteService;
import com.juridiqsystem.crm.service.exceptions.StandardError;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @Operation(summary = "Listar participantes (paginado)", description = "Requer JWT. Mesma listagem de GET /participante, porém paginada (parâmetros padrão do Spring Data: `page`, `size`, `sort`). Recomendado para telas novas — a tabela de participantes cresce com todo contato recebido via WhatsApp.")
    @ApiResponse(responseCode = "200", description = "Página de participantes")
    @GetMapping(value = "/paginado")
    public Page<ParticipanteDTO> findAllPaginado(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return participanteService.findAllPaginado(pageable);
    }

    @Operation(summary = "Listar participantes sem protocolo aberto com outro administrador", description = "Requer JWT. Retorna os participantes disponíveis para abertura de um novo protocolo com o admin `id` (exclui participantes que já possuem protocolo ABERTO com outro administrador).")
    @ApiResponse(responseCode = "200", description = "Lista filtrada de participantes", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ParticipanteDTO.class))))
    @GetMapping(value = "/filter/{id}")
    public List<ParticipanteDTO> findAllFilter(@Parameter(description = "Id do administrador de referência", required = true) @PathVariable String id) {

        return participanteService.findAllFilter(id);
    }

    @Operation(summary = "Buscar participante por id", description = "Requer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante encontrado", content = @Content(schema = @Schema(implementation = ParticipanteDTO.class))),
            @ApiResponse(responseCode = "400", description = "Participante não encontrado para o id informado. Resposta em JSON estruturado (StandardError) — UsernameNotFoundException cai no handler genérico de RuntimeException.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class)))
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@Parameter(description = "Id do participante", required = true) @PathVariable String id) {
        ParticipanteDTO resposta = participanteService.findById(id);
        return ResponseEntity.ok(resposta);
    }

    @Operation(summary = "Criar participante", description = "Requer JWT. Cria um novo participante; o login é normalizado para minúsculas e a foto/avatar recebe o valor padrão do sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, ou violação de constraint única (login ou CPF já cadastrado) no banco de dados — JSON StandardError, produzido pelo handler de org.springframework.dao.DataIntegrityViolationException.",
                    content = @Content(schema = @Schema(implementation = StandardError.class)))
    })
    @PostMapping
    public ResponseEntity<?> post(@Parameter(description = "Dados do participante a ser criado", required = true) @RequestBody @Valid ParticipanteCreateRequest participante) {
        participanteService.create(participante);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Atualizar participante", description = "Requer JWT. Atualiza os dados cadastrais do participante identificado por `id`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante atualizado", content = @Content(schema = @Schema(implementation = ParticipanteDTO.class))),
            @ApiResponse(responseCode = "400", description = "Três causas possíveis, todas em JSON: (1) dados inválidos — mapa simples campo -> mensagem; (2) participante não encontrado — StandardError (UsernameNotFoundException, capturado pelo handler genérico de RuntimeException); (3) login/CPF em conflito com outro registro — StandardError (org.springframework.dao.DataIntegrityViolationException)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class)))
    })
    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@Parameter(description = "Id do participante", required = true) @PathVariable String id, @Parameter(description = "Dados atualizados do participante", required = true) @RequestBody @Valid ParticipanteUpdateRequest participante) {
        Participante participanteResposta = participanteService.update(participante,id);
        return ResponseEntity.ok(new ParticipanteDTO(participanteResposta));
    }

    @Operation(summary = "Excluir participante", description = "Requer JWT. Exclusão física (hard delete) do participante identificado por `id`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Participante não encontrado para o id informado. Resposta em JSON estruturado (StandardError).",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class)))
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "Id do participante", required = true) @PathVariable String id) {
        participanteService.delete(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Buscar participante por celular", description = "Requer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante encontrado", content = @Content(schema = @Schema(implementation = ParticipanteDTO.class))),
            @ApiResponse(responseCode = "400", description = "Nenhum participante cadastrado com esse celular. Resposta em JSON estruturado (StandardError).",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class)))
    })
    @GetMapping(value = "/celular/{celular}")
    public ResponseEntity<?> findByCelular(@Parameter(description = "Número de celular do participante", required = true) @PathVariable String celular) {
        return ResponseEntity.ok(new ParticipanteDTO(participanteService.findByCelular(celular)));
    }

    @Operation(summary = "Buscar participante por login", description = "Requer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participante encontrado", content = @Content(schema = @Schema(implementation = ParticipanteDTO.class))),
            @ApiResponse(responseCode = "400", description = "Nenhum participante cadastrado com esse login. Resposta em JSON estruturado (StandardError).",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StandardError.class)))
    })
    @GetMapping(value = "/login/{login}")
    public ResponseEntity<?> findByLogin(@Parameter(description = "Login (e-mail) do participante", required = true) @PathVariable String login) {
        return ResponseEntity.ok(participanteService.findByLogin(login));
    }


}

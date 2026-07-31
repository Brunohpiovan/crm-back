package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.FiltroFunilDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.FunilAllDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.FunilCreateRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.FunilDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioContatoDto;
import br.edu.faculdadevincit.crm_vincit.service.FunilService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Funil", description = "Funis de vendas (pipelines): CRUD, filtro por situação/tags das oportunidades e associação de funcionários responsáveis pelo funil.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/funil")
public class FunilController {

    @Autowired
    private FunilService funilService;

    @Operation(summary = "Listar funis visíveis para o usuário autenticado",
            description = "Requer JWT. Administradores recebem todos os funis cadastrados; demais cargos recebem apenas os funis aos quais estão associados como funcionários.")
    @ApiResponse(responseCode = "200", description = "Lista de funis",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = FunilAllDTO.class))))
    @GetMapping
    public List<FunilAllDTO> findAll() {
        return funilService.findAll();
    }

    @Operation(summary = "Listar funcionários disponíveis para entrar em um funil",
            description = "Requer JWT. Retorna os usuários (não administradores) que ainda não fazem parte do funil informado, candidatos a serem adicionados via POST /funil/add-funcionario/{funilId}/{funcionarioId}.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuários disponíveis para o funil",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UsuarioContatoDto.class)))),
            @ApiResponse(responseCode = "400", description = "Funil não encontrado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Funil não encontrado")))
    })
    @GetMapping("/no-funil/{funilId}")
    public ResponseEntity<List<UsuarioContatoDto>> findFuncionarios(@Parameter(description = "Id do funil", required = true) @PathVariable Long funilId) {
        List<UsuarioContatoDto> funcionarios = funilService.findFuncionariosFunil(funilId);
        return ResponseEntity.ok(funcionarios);
    }

    @Operation(summary = "Buscar funil por id",
            description = "Requer JWT. Retorna o funil com suas etapas e, dentro de cada etapa, as oportunidades correspondentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funil encontrado",
                    content = @Content(schema = @Schema(implementation = FunilDto.class))),
            @ApiResponse(responseCode = "400", description = "Funil não encontrado para o id informado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Funil com id 1 não encontrado")))
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<?> findById(@Parameter(description = "Id do funil", required = true) @PathVariable Long id) {
        FunilDto funil = funilService.findById(id);
        return ResponseEntity.ok(funil);
    }

    @Operation(summary = "Buscar funil filtrando as oportunidades por situação e/ou tags",
            description = "Requer JWT. Retorna o mesmo formato de GET /funil/{id}, mas com as oportunidades de cada etapa já filtradas pelas situações e/ou tags informadas no corpo da requisição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funil encontrado (com oportunidades já filtradas)",
                    content = @Content(schema = @Schema(implementation = FunilDto.class))),
            @ApiResponse(responseCode = "404", description = "Nenhum funil encontrado com o id informado no filtro (resposta sem corpo)")
    })
    @PostMapping("/filtro")
    public ResponseEntity<?> findByIdAndSituacao(@RequestBody FiltroFunilDto filtro) {
        FunilDto funil = funilService.findByIdAndSituacao(filtro.getId(), filtro.getSituacoes(),filtro.getTags());
        return funil != null ? ResponseEntity.ok(funil) : ResponseEntity.notFound().build();
    }


    @Operation(summary = "Criar um novo funil", description = "Requer JWT. Cria um funil vazio (sem etapas) a partir dos dados informados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funil criado com sucesso",
                    content = @Content(schema = @Schema(implementation = FunilDto.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (ex.: nome ausente ou em branco)")
    })
    @PostMapping
    public ResponseEntity<FunilDto> create(@RequestBody @Valid FunilCreateRequest funilCreateRequest) {
        FunilDto dto = funilService.create(funilCreateRequest);
        return ResponseEntity.ok(dto);
    }


    @Operation(summary = "Adicionar um funcionário a um funil",
            description = "Requer JWT. Associa o usuário (funcionário) informado ao funil informado, liberando o acesso dele a esse pipeline. A operação é idempotente: se o funcionário já pertencer ao funil, nada é alterado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funcionário adicionado ao funil com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400", description = "Funil ou funcionário não encontrado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Funil não encontrado")))
    })
    @PostMapping("/add-funcionario/{funilId}/{funcionarioId}")
    public ResponseEntity<FunilDto> addFuncionario(@Parameter(description = "Id do funil", required = true) @PathVariable Long funilId,
                                                    @Parameter(description = "Id do usuário/funcionário a ser adicionado", required = true) @PathVariable Long funcionarioId) {
        funilService.adicionarFuncionarioFunil(funcionarioId,funilId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Atualizar o nome de um funil", description = "Requer JWT. Atualiza os dados básicos (nome) de um funil existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funil atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = FunilAllDTO.class))),
            @ApiResponse(responseCode = "400", description = "Funil não encontrado para o id informado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Funil com id 1 não encontrado")))
    })
    @PutMapping(value = "/{id}")
    public ResponseEntity<?> update(@Parameter(description = "Id do funil", required = true) @PathVariable Long id,@RequestBody FunilAllDTO funilRequest ) {
        FunilAllDTO funil = funilService.update(id, funilRequest);
        return ResponseEntity.ok(funil);
    }

    @Operation(summary = "Excluir um funil", description = "Requer JWT. Remove o funil e, em cascata, todas as suas etapas e oportunidades associadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funil excluído com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400", description = "Funil não encontrado para o id informado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Funil com id 1 não encontrado")))
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "Id do funil", required = true) @PathVariable Long id) {
        funilService.delete(id);
        return ResponseEntity.ok().build();
    }
}

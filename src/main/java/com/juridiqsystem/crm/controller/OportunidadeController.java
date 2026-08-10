package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.MoveOportunidadeDTO;
import com.juridiqsystem.crm.model.dtos.OportunidadeCreateRequest;
import com.juridiqsystem.crm.model.dtos.OportunidadeDTO;
import com.juridiqsystem.crm.model.dtos.OportunidadeUpdateRequest;
import com.juridiqsystem.crm.service.OportunidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Oportunidade", description = "Oportunidades de venda (cards do funil): CRUD com upload opcional de anexo (multipart) e movimentação entre etapas/funis. A maioria das operações de escrita publica o novo estado em tópicos WebSocket para atualização em tempo real de outros clientes conectados.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/oportunidade")
public class OportunidadeController {

    @Autowired
    private OportunidadeService oportunidadeService;

    @Operation(summary = "Listar oportunidades (paginado)",
            description = "Requer JWT. Retorna todas as oportunidades do sistema em formato paginado (parâmetros padrão do Spring Data: page, size, sort). Tamanho de página padrão: 20.")
    @ApiResponse(responseCode = "200", description = "Página de oportunidades",
            content = @Content(schema = @Schema(implementation = OportunidadeDTO.class)))
    @GetMapping
    public Page<OportunidadeDTO> findAll(@Parameter(description = "Parâmetros de paginação (page, size, sort)") @PageableDefault(size = 20) Pageable pageable) {
        return oportunidadeService.findAll(pageable);
    }

    @Operation(summary = "Buscar oportunidade por id", description = "Requer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oportunidade encontrada",
                    content = @Content(schema = @Schema(implementation = OportunidadeDTO.class))),
            @ApiResponse(responseCode = "400", description = "Oportunidade não encontrada para o id informado (resposta em JSON estruturado — StandardError)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Oportunidade com id 1 nao encontrada")))
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<OportunidadeDTO> findById(@Parameter(description = "Id da oportunidade", required = true) @PathVariable String id) {
        return ResponseEntity.ok(oportunidadeService.findById(id));
    }

    @Operation(summary = "Buscar a oportunidade \"solta\" de um cliente (sem criador definido)",
            description = "Requer JWT. Usado para localizar a oportunidade gerada automaticamente para um cliente/participante que ainda não foi assumida por nenhum vendedor (criador nulo). Retorna 200 com corpo `null` caso não exista nenhuma oportunidade nessas condições para o cliente informado (não é um erro).")
    @ApiResponse(responseCode = "200", description = "Oportunidade encontrada, ou null se o cliente não possuir oportunidade sem criador",
            content = @Content(schema = @Schema(implementation = OportunidadeDTO.class)))
    @GetMapping(value = "/cliente/{id}")
    public ResponseEntity<OportunidadeDTO> findByClienteAndCriadorNull(@Parameter(description = "Id do cliente/participante", required = true) @PathVariable String id) {
        return ResponseEntity.ok(oportunidadeService.findByClienteAndCriadorNull(id));
    }

    @Operation(summary = "Criar uma nova oportunidade",
            description = "Requer JWT. Requisição multipart/form-data: a parte `oportunidade` contém o JSON com os dados da oportunidade (título, etapa, criador, cliente, valor, tags etc.) e a parte `file`, opcional, é uma imagem (PNG, JPG, JPEG, WEBP ou GIF, até 100 MB) usada como anexo. O cliente informado é resolvido por celular: se já existir um participante com o mesmo celular ele é reaproveitado/atualizado, senão um novo é criado. Após a criação, o backend publica a nova oportunidade no canal WebSocket /topic/newoportunidade para atualização em tempo real de outros clientes conectados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oportunidade criada com sucesso",
                    content = @Content(schema = @Schema(implementation = OportunidadeDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Duas causas possíveis, com formatos de corpo diferentes: (1) erro de validação Bean Validation dos campos de `oportunidade` (mapa simples campo -> mensagem); ou (2) etapa/criador/tag não encontrado, ou arquivo de anexo inválido — tamanho acima de 100 MB ou tipo de imagem não permitido (resposta em JSON estruturado — StandardError, capturado pelo @ExceptionHandler(RuntimeException.class) do ResourceExceptionHandler).",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"campo\": \"mensagem de erro\"}")),
                            @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Tipo de arquivo não permitido. Envie uma imagem (PNG, JPG, JPEG, WEBP ou GIF)."))
                    })
    })
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<OportunidadeDTO> create(
            @Parameter(description = "Dados da oportunidade a ser criada, em JSON", required = true) @RequestPart("oportunidade") @Valid OportunidadeCreateRequest oportunidade,
            @Parameter(description = "Arquivo de imagem opcional a ser anexado à oportunidade (PNG, JPG, JPEG, WEBP ou GIF, até 100 MB)") @RequestPart(value = "file", required = false) MultipartFile file) {

        OportunidadeDTO dto = oportunidadeService.create(oportunidade, file);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Atualizar uma oportunidade",
            description = "Requer JWT. Requisição multipart/form-data, mesmo formato de POST /oportunidade. Se um novo `file` for enviado, o anexo anterior (se houver) é removido do armazenamento e substituído; se `urlAnexo` vier nulo e nenhum `file` for enviado, o anexo existente é removido. Se a etapa informada for diferente da etapa atual, os valores totais das etapas de origem/destino são recalculados e a oportunidade tem sua data de entrada na etapa atualizada. Após a atualização, o backend publica o novo estado da oportunidade no canal WebSocket /topic/newoportunidade (quando a etapa mudou) ou /topic/updateOportunidade (quando a etapa não mudou), para atualização em tempo real de outros clientes conectados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oportunidade atualizada com sucesso",
                    content = @Content(schema = @Schema(implementation = OportunidadeDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Duas causas possíveis, com formatos de corpo diferentes: (1) erro de validação Bean Validation dos campos de `oportunidade` (mapa simples campo -> mensagem); ou (2) oportunidade/etapa/criador/cliente/tag não encontrado, ou arquivo de anexo inválido — tamanho acima de 100 MB ou tipo de imagem não permitido (resposta em JSON estruturado — StandardError, capturado pelo @ExceptionHandler(RuntimeException.class) do ResourceExceptionHandler).",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"campo\": \"mensagem de erro\"}")),
                            @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Oportunidade com id 1 nao encontrada"))
                    })
    })
    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<OportunidadeDTO> update(
            @Parameter(description = "Id da oportunidade a ser atualizada", required = true) @PathVariable String id,
            @Parameter(description = "Dados atualizados da oportunidade, em JSON", required = true) @RequestPart("oportunidade") @Valid OportunidadeUpdateRequest oportunidade,
            @Parameter(description = "Novo arquivo de imagem opcional para substituir o anexo atual (PNG, JPG, JPEG, WEBP ou GIF, até 100 MB)") @RequestPart(value = "file", required = false) MultipartFile file) {

        OportunidadeDTO dto = oportunidadeService.update(id, oportunidade, file);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Mover uma oportunidade entre etapas (drag-and-drop no Kanban)",
            description = "Requer JWT. Move a oportunidade para a etapa informada, posicionando-a no índice indicado dentro da lista de oportunidades da etapa de destino (reindexação das demais oportunidades da(s) etapa(s) afetada(s)). Se a etapa de destino for diferente da etapa atual, os valores totais das duas etapas são recalculados. Após a operação, o backend publica: os ids da oportunidade e da etapa de destino (payload enxuto) no canal WebSocket /topic/movimentoOportunidade, e a lista reordenada de oportunidades de cada etapa afetada no canal /topic/attoportunidades — para atualização em tempo real de outros clientes conectados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oportunidade movida com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400",
                    description = "Duas causas possíveis, com formatos de corpo diferentes: (1) erro de validação Bean Validation dos campos do corpo da requisição (mapa simples campo -> mensagem); ou (2) oportunidade/etapa não encontrada, ou oportunidade sem etapa definida (resposta em JSON estruturado — StandardError, capturado pelo @ExceptionHandler(RuntimeException.class) do ResourceExceptionHandler).",
                    content = {
                            @Content(mediaType = "application/json", schema = @Schema(type = "object", example = "{\"campo\": \"mensagem de erro\"}")),
                            @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Oportunidade não encontrada"))
                    })
    })
    @PutMapping("/move")
    public ResponseEntity<?> move(@RequestBody @Valid MoveOportunidadeDTO dto) {
        oportunidadeService.movimentoOportunidade(dto.getOportunidadeId(), dto.getEtapaId(), dto.getIndice());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Excluir uma oportunidade",
            description = "Requer JWT. Remove a oportunidade e desconta seu valor do total acumulado da etapa em que estava. Após a exclusão, o backend publica o id da oportunidade removida no canal WebSocket /topic/deletedoportunidade, para atualização em tempo real de outros clientes conectados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oportunidade excluída com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400", description = "Oportunidade não encontrada para o id informado (resposta em JSON estruturado — StandardError)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Oportunidade com id 1 nao encontrada")))
    })
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "Id da oportunidade", required = true) @PathVariable String id) {
        oportunidadeService.delete(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mover uma oportunidade para a lixeira",
            description = "Requer JWT. Soft delete: marca a oportunidade com situação LIXEIRA e desconta seu valor do total acumulado da etapa em que estava, sem apagar a linha do banco (o board não lista oportunidades LIXEIRA por padrão, então ela some da tela). Após a operação, o backend publica o id da oportunidade no canal WebSocket /topic/deletedoportunidade, para atualização em tempo real de outros clientes conectados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oportunidade movida para a lixeira com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400", description = "Oportunidade não encontrada para o id informado (resposta em JSON estruturado — StandardError)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Oportunidade com id 1 nao encontrada")))
    })
    @PutMapping(value = "/{id}/lixeira")
    public ResponseEntity<?> moverParaLixeira(@Parameter(description = "Id da oportunidade", required = true) @PathVariable String id) {
        oportunidadeService.moverParaLixeira(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Restaurar uma oportunidade da lixeira",
            description = "Requer JWT. Marca a oportunidade novamente como ABERTO e devolve seu valor ao total acumulado da etapa em que estava. Após a operação, o backend publica a oportunidade no canal WebSocket /topic/newoportunidade, para que volte a aparecer em tempo real no board de outros clientes conectados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Oportunidade restaurada com sucesso (resposta sem corpo)"),
            @ApiResponse(responseCode = "400", description = "Oportunidade não encontrada para o id informado (resposta em JSON estruturado — StandardError)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Oportunidade com id 1 nao encontrada")))
    })
    @PutMapping(value = "/{id}/restaurar")
    public ResponseEntity<?> restaurar(@Parameter(description = "Id da oportunidade", required = true) @PathVariable String id) {
        oportunidadeService.restaurar(id);
        return ResponseEntity.ok().build();
    }
}

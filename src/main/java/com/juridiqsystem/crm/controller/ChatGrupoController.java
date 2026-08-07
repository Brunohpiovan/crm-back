package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.Oportunidade;
import com.juridiqsystem.crm.model.dtos.ChatGrupoResponseById;
import com.juridiqsystem.crm.model.dtos.ChatGrupoResponseDTO;
import com.juridiqsystem.crm.model.dtos.GrupoCreateDTO;
import com.juridiqsystem.crm.model.dtos.OportunidadeDTO;
import com.juridiqsystem.crm.service.ChatGrupoService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Chat em Grupo", description = "Criação, atualização e consulta de grupos de chat (públicos e privados). Criar/atualizar um grupo dispara notificações em tempo real via WebSocket (STOMP) para os usuários envolvidos.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/grupos")
public class ChatGrupoController {

    @Autowired
    private ChatGrupoService chatGrupoService;

    @Operation(
            summary = "Buscar grupo privado entre dois usuários",
            description = "Retorna o id do grupo de chat privado (1-a-1) existente entre `idUsuario1` e `idUsuario2`, se houver. Não cria o grupo caso ele não exista."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Id do grupo privado encontrado",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "204", description = "Não existe grupo privado entre os dois usuários informados (ou algum dos usuários não existe)")
    })
    @GetMapping("/{idUsuario1}/{idUsuario2}")
    public ResponseEntity<?> getProtocoloByUsuario(
            @Parameter(description = "Id do primeiro usuário", required = true) @PathVariable String idUsuario1,
            @Parameter(description = "Id do segundo usuário", required = true) @PathVariable String idUsuario2) {
        return chatGrupoService.getGrupoByUsuario(idUsuario1, idUsuario2)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }


    @Operation(
            summary = "Listar grupos públicos de um usuário",
            description = "Retorna os grupos públicos dos quais o usuário identificado por `idUsuario` participa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de grupos públicos do usuário",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatGrupoResponseDTO.class)))),
            @ApiResponse(responseCode = "204", description = "Usuário não participa de nenhum grupo público"),
            @ApiResponse(responseCode = "400", description = "Usuário não encontrado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não encontrado")))
    })
    @GetMapping("/{idUsuario}")
    public ResponseEntity<?> getProtocoloByUsuarioAndPublic(@Parameter(description = "Id do usuário", required = true) @PathVariable String idUsuario) {
        return chatGrupoService.getGrupoByUsuarioAndPublic(idUsuario)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }


    @Operation(
            summary = "Buscar grupo por id",
            description = "Retorna os dados completos do grupo (incluindo a lista de ids dos usuários participantes)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grupo encontrado",
                    content = @Content(schema = @Schema(implementation = ChatGrupoResponseById.class))),
            @ApiResponse(responseCode = "400", description = "Grupo não encontrado para o id informado (resposta em texto puro, não JSON estruturado)",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "grupo nao encontrado")))
    })
    @GetMapping("/id/{idUsuario}")
    public ResponseEntity<?> findById(@Parameter(description = "Id do grupo", required = true) @PathVariable String idUsuario){
        ChatGrupoResponseById grupo = chatGrupoService.findById(idUsuario);
        return ResponseEntity.ok(grupo);
    }


    @Operation(
            summary = "Criar grupo de chat público",
            description = """
                    Cria um novo grupo de chat público. Requisição multipart/form-data com três partes: \
                    `grupo` (JSON com os dados do grupo), `foto` (opcional, imagem de avatar do grupo) e \
                    `imagemFundo` (opcional, imagem de fundo do grupo). Se nenhuma foto for enviada, um \
                    avatar padrão é usado. As imagens enviadas são armazenadas no S3.

                    Após a criação, uma notificação é publicada via WebSocket no tópico \
                    `/topic/newPublicGroup/{userId}` para cada usuário incluído no grupo, com o payload \
                    do grupo criado (`ChatGrupoResponseDTO`), para que o frontend atualize a lista de \
                    grupos em tempo real.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grupo criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, usuário(s) informado(s) não encontrado(s) ou falha no upload da imagem para o S3 (resposta em texto puro, não JSON estruturado; mensagem varia conforme a causa, ex.: \"Usuário(s) com ID [3] não encontrado(s)\", \"O arquivo está vazio\" ou \"A transferência falhou: ...\")",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário(s) com ID [3] não encontrado(s)")))
    })
    @PostMapping
    public ResponseEntity<?> create(
            @Parameter(description = "Dados do grupo em JSON (parte multipart 'grupo': nome, lista de ids de usuários, etc.)", required = true)
            @RequestPart("grupo") GrupoCreateDTO grupoCreateDTO,
            @Parameter(description = "Arquivo de imagem para o avatar do grupo (parte multipart opcional)")
            @RequestPart(value = "foto", required = false) MultipartFile foto,
            @Parameter(description = "Arquivo de imagem para o plano de fundo do grupo (parte multipart opcional)")
            @RequestPart(value = "imagemFundo", required = false) MultipartFile imagemFundo) {
            chatGrupoService.create(grupoCreateDTO, foto, imagemFundo);
            return ResponseEntity.ok().build();
    }


    @Operation(
            summary = "Atualizar grupo de chat",
            description = """
                    Atualiza nome, avatar, imagem de fundo e lista de participantes de um grupo existente. \
                    Requisição multipart/form-data com as mesmas três partes do endpoint de criação. Se \
                    uma nova `foto`/`imagemFundo` for enviada, a antiga é removida do S3 (quando aplicável) \
                    e substituída; se nenhuma imagem for enviada e o grupo não tinha uma customizada, o \
                    avatar padrão é mantido.

                    Após a atualização, uma notificação é publicada via WebSocket em \
                    `/topic/attPublicGroup/{userId}` (payload `ChatGrupoResponseDTO`) para cada usuário que \
                    permanece no grupo, e em `/topic/delPublicGroup/{userId}` (payload: id do grupo) para \
                    cada usuário que foi removido do grupo nessa atualização.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grupo atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Grupo ou usuário(s) não encontrado(s), ou falha no upload/remoção da imagem no S3 (resposta em texto puro, não JSON estruturado; mensagem varia conforme a causa, ex.: \"grupo nao encontrado\", \"Usuário(s) com ID [3] não encontrado(s)\" ou \"Falha na deleção do objeto: ...\")",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "grupo nao encontrado")))
    })
    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> update(
            @Parameter(description = "Id do grupo a ser atualizado", required = true) @PathVariable String id,
            @Parameter(description = "Dados do grupo em JSON (parte multipart 'grupo')", required = true)
            @RequestPart("grupo") GrupoCreateDTO grupo,
            @Parameter(description = "Novo arquivo de imagem para o avatar do grupo (parte multipart opcional)")
            @RequestPart(value = "foto", required = false) MultipartFile file,
            @Parameter(description = "Novo arquivo de imagem para o plano de fundo do grupo (parte multipart opcional)")
            @RequestPart(value = "imagemFundo", required = false) MultipartFile imagemFundo) {

        chatGrupoService.update(id, grupo, file,imagemFundo);
        return ResponseEntity.ok().build();
    }
}

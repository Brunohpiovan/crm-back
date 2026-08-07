package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.MensagemResponseDTO;
import com.juridiqsystem.crm.service.MensagemService;
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

@Tag(name = "Mensagem", description = "Histórico de mensagens de chat (WhatsApp) vinculadas a um protocolo de atendimento ou ainda sem protocolo associado (\"públicas\").")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class MensagemController {

    private static final int LIMIT_MAXIMO = 100;

    @Autowired
    private MensagemService mensagemService;

    @Operation(
            summary = "Listar todas as mensagens de um protocolo",
            description = """
                    Requer JWT. Retorna todo o histórico de mensagens do protocolo `protocoloId`, \
                    sem paginação. O usuário autenticado precisa ser o administrador ou o \
                    participante desse protocolo. Rota legada/duplicada: existe também \
                    GET /api/messages/{protocoloId}, que faz a mesma busca mas com suporte a \
                    paginação (offset/limit); ambas foram mantidas por compatibilidade com \
                    diferentes versões do frontend.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de mensagens do protocolo", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MensagemResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Protocolo não encontrado, admin autenticado não encontrado ou usuário não autorizado a acessar este protocolo")
    })
    @GetMapping("/messages/{protocoloId}")
    public List<MensagemResponseDTO> getMessages(@Parameter(description = "Id do protocolo", required = true) @PathVariable String protocoloId) {
        List<MensagemResponseDTO> messages = mensagemService.getMessagesForProtocol(protocoloId);
        return messages;
    }

    @Operation(
            summary = "Listar mensagens de um protocolo (paginado)",
            description = """
                    Requer JWT. Mesma checagem de autorização de GET /messages/{protocoloId} \
                    (usuário autenticado precisa ser o admin ou o participante do protocolo), \
                    porém com paginação via `offset`/`limit` e ordenação decrescente por id \
                    (mensagens mais recentes primeiro). Rota legada/duplicada em relação a \
                    /messages/{protocoloId}, mantida por compatibilidade.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de mensagens do protocolo", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MensagemResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Protocolo não encontrado, admin autenticado não encontrado ou usuário não autorizado a acessar este protocolo")
    })
    @GetMapping("/api/messages/{protocoloId}")
    public List<MensagemResponseDTO> getMessagesLimit(@Parameter(description = "Id do protocolo", required = true) @PathVariable String protocoloId,
                                                      @Parameter(description = "Deslocamento (offset) usado para calcular a página: page = offset / limit") @RequestParam(defaultValue = "0") int offset,
                                                      @Parameter(description = "Quantidade máxima de mensagens por página (limitado a " + LIMIT_MAXIMO + ")") @RequestParam(defaultValue = "10") int limit) {
        int limiteValido = Math.max(1, Math.min(limit, LIMIT_MAXIMO));
        return mensagemService.getMessagesForProtocolLimit(protocoloId, offset, limiteValido);
    }

    @Operation(summary = "Listar mensagens sem protocolo (públicas) de um remetente", description = "Requer JWT. Retorna as mensagens enviadas por `userId` (id do Participante) que ainda não foram vinculadas a nenhum protocolo — típico de contatos que ainda não têm atendimento aberto.")
    @ApiResponse(responseCode = "200", description = "Lista de mensagens sem protocolo", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MensagemResponseDTO.class))))
    @GetMapping("/messages/public/{userId}")
    public List<MensagemResponseDTO> getMessagesPublic(@Parameter(description = "Id do Participante remetente", required = true) @PathVariable String userId) {
        return mensagemService.getMessagesPublic(userId);
    }

    @Operation(summary = "Listar ids de remetentes com mensagens sem protocolo", description = "Requer JWT. Retorna os ids distintos de Participante que possuem ao menos uma mensagem ainda não vinculada a um protocolo, útil para o frontend saber quais contatos precisam de atendimento.")
    @ApiResponse(responseCode = "200", description = "Lista de ids de participantes", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @GetMapping("messages/public/sender-ids")
    public ResponseEntity<List<String>> getSenderIdsPublic() {
        List<String> senderIds = mensagemService.getSenderIdsWithoutProtocol();
        return ResponseEntity.ok(senderIds);
    }


    @Operation(summary = "Excluir mensagens sem protocolo de um remetente", description = "Requer JWT. Remove permanentemente todas as mensagens de `userId` (Participante) que ainda não estavam vinculadas a nenhum protocolo — usado, por exemplo, após o atendimento delas ser formalmente aberto/tratado de outra forma.")
    @ApiResponse(responseCode = "200", description = "Mensagens excluídas com sucesso")
    @DeleteMapping("/messages/public/{userId}")
    public void deletePublicMessage(@Parameter(description = "Id do Participante remetente", required = true) @PathVariable String userId){
        mensagemService.deleteMessagesWithoutProtocol(userId);
    }
}

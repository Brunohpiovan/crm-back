package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Mensagem;
import br.edu.faculdadevincit.crm_vincit.model.MensagemInterna;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemInternaResponseDTO;
import br.edu.faculdadevincit.crm_vincit.service.MensagemInternaService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Mensagem Interna", description = "Histórico de mensagens do chat interno de um grupo (mensagens já enviadas; o envio em tempo real acontece via WebSocket, documentado separadamente).")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class MensagemInternaController {

    @Autowired
    private MensagemInternaService mensagemInternaService;

    @Operation(
            summary = "Listar histórico de mensagens de um grupo (paginado)",
            description = """
                    Retorna uma página do histórico de mensagens internas do grupo `grupoId`, ordenadas \
                    da mais recente para a mais antiga (`id` descendente). A paginação é feita por \
                    `offset`/`limit` simples (não é um `Pageable` do Spring): `offset` é convertido \
                    internamente em número de página via `offset / limit`. O usuário autenticado (extraído \
                    do JWT) precisa ser um dos participantes do grupo, caso contrário a requisição é rejeitada.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de mensagens do grupo",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MensagemInternaResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Grupo não encontrado, usuário autenticado não encontrado, ou usuário não é participante do grupo (resposta em texto puro, não JSON estruturado; mensagem varia conforme a causa, ex.: \"Grupo não encontrado\", \"Admin não encontrado\" ou \"Usuário não autorizado a acessar este protocolo.\")",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Usuário não autorizado a acessar este protocolo.")))
    })
    @GetMapping("/messagesInterna/{grupoId}")
    public List<MensagemInternaResponseDTO> getMessagesLimit(
            @Parameter(description = "Id do grupo de chat", required = true) @PathVariable Long grupoId,
            @Parameter(description = "Deslocamento (offset) usado para calcular a página: página = offset / limit") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "Quantidade máxima de mensagens retornadas na página") @RequestParam(defaultValue = "10") int limit) {
        return mensagemInternaService.getMessagesForProtocolLimit(grupoId, offset, limit);
    }
}

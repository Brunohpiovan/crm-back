package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.ContatoDTO;
import br.edu.faculdadevincit.crm_vincit.service.ContatoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Contato (Landing Page)", description = "Recebimento de leads/contatos vindos do site institucional (fora do CRM).")
@RestController
@RequestMapping("/api")
public class ContatoController {

    @Autowired
    private ContatoService contatoService;

    @Operation(
            summary = "Registrar contato/lead do site institucional",
            description = """
                    Endpoint público, usado pelo formulário de contato do site institucional \
                    (não é o Angular do CRM). Cria um novo contato/lead a partir de nome, e-mail, \
                    celular e interesse informados.
                    """
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contato registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes ou inválidos (nome, email, celular, interesse). " +
                    "Retorna um mapa simples {\"campo\": \"mensagem\"} por campo inválido (não segue o formato StandardError).",
                    content = @Content(schema = @Schema(type = "object", example = "{\"nome\": \"O nome é obrigatório e não pode estar em branco\"}")))
    })
    @PostMapping(value = "/contato")
    public ResponseEntity<?> create(@RequestBody @Valid ContatoDTO contatoDTO) {
        contatoService.create(contatoDTO);
        return ResponseEntity.ok().build();
    }
}

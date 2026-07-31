package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.AuthenticationDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.LoginResponseDTO;
import br.edu.faculdadevincit.crm_vincit.service.auth.AuthenticationService;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação", description = "Login e emissão de token JWT usado nos demais endpoints da API.")
@RestController
@RequestMapping("auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService service;

    @Operation(
            summary = "Login (obter token JWT)",
            description = """
                    Endpoint público (não exige header Authorization). Recebe login e senha e, \
                    se válidos, retorna um token JWT válido por 12 horas.

                    O token retornado deve ser enviado em todas as demais requisições no header:
                    `Authorization: Bearer {token}`

                    Também é retornado `logId`, o identificador do registro de acesso (log) criado \
                    para esta sessão — usado posteriormente em `PUT /log/finalizar/{id}` para \
                    marcar o horário de saída quando o usuário faz logout.
                    """
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado com sucesso",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Login ou senha inválidos. Resposta em **texto puro** (não é um JSON estruturado) — " +
                    "corpo é diretamente a mensagem de erro, ex.: \"Bad credentials\".",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Bad credentials"))),
            @ApiResponse(responseCode = "403", description = "Usuário bloqueado pelo administrador (campo `bloqueado` do usuário)",
                    content = @Content(schema = @Schema(implementation = br.edu.faculdadevincit.crm_vincit.model.dtos.ApiResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthenticationDTO data){
        return ResponseEntity.ok(service.login(data));
    }

}
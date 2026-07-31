package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.dtos.ApiResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EmailDTO;
import br.edu.faculdadevincit.crm_vincit.service.auth.PasswordSendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Login e emissão de token JWT usado nos demais endpoints da API.")
@RestController
public class PasswordRecoveryController {

    @Autowired
    private PasswordSendService service;

    @Operation(
            summary = "Solicitar recuperação de senha",
            description = """
                    Endpoint público. Envia, para o e-mail informado (caso exista um usuário \
                    cadastrado com esse e-mail), uma mensagem com um link/token para redefinição \
                    de senha, a ser usado em `POST /reset-password`.

                    Por segurança, a resposta é genérica e não indica se o e-mail existe ou não \
                    na base de usuários.
                    """
    )
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Solicitação processada (e-mail enviado, se existir usuário correspondente)",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Falha inesperada ao processar a solicitação (ex.: falha no envio de e-mail). " +
            "Resposta em **texto puro** (não é um JSON estruturado)",
            content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Erro interno no servidor. Tente novamente mais tarde.")))
    @PostMapping("/recover-password")
    public ResponseEntity<ApiResponse> recoverPassword(@RequestBody EmailDTO email) {
        return ResponseEntity.ok(service.SendEmailRecovery(email));
    }
}
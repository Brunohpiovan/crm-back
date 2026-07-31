package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.PasswordChangeRequest;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ApiResponse;
import br.edu.faculdadevincit.crm_vincit.service.auth.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação", description = "Login e emissão de token JWT usado nos demais endpoints da API.")
@RestController
public class PasswordResetController {

    @Autowired
    private PasswordResetService service;

    @Operation(
            summary = "Redefinir senha com token de recuperação",
            description = """
                    Endpoint público. Recebe o `token` enviado por e-mail em `POST /recover-password`, \
                    a nova senha (`password`) e sua confirmação (`password2`, deve ser idêntica a \
                    `password`), e efetiva a troca de senha do usuário associado ao token.
                    """
    )
    @SecurityRequirements
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Três formatos possíveis dependendo da causa: " +
            "(1) token inválido/expirado ou senhas não coincidem → JSON `{\"message\": \"...\"}` (schema ApiResponse, mostrado abaixo); " +
            "(2) usuário do token não encontrado (caso raro) → **texto puro** com a mensagem de erro; " +
            "(3) campos ausentes ou com menos de 8 caracteres (falha de `@Valid`) → mapa simples `{\"campo\": \"mensagem\"}`.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody PasswordChangeRequest request) {
        return ResponseEntity.ok(service.changePassord(request.getToken(),request.getPassword(), request.getPassword2()));
    }

}
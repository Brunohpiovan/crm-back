package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.service.escavador.EscavadorCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Escavador Webhook", description = "Endpoint público que recebe os callbacks da Escavador.")
@RestController
@RequestMapping("/webhooks/escavador")
public class EscavadorWebhookController {

    @Autowired
    private EscavadorCallbackService escavadorCallbackService;

    @Operation(
            summary = "Callback de eventos de monitoramento (uso exclusivo da Escavador)",
            description = """
                    Endpoint PÚBLICO (não exige JWT) — chamado pela infraestrutura da Escavador \
                    quando há novidade em um processo monitorado. A autenticidade é validada por \
                    um token secreto que nós geramos no painel da API da Escavador e que ela envia \
                    no header `Authorization` (o mesmo token também é aceito em `?token=`, para o \
                    caso de ser embutido na própria URL de callback). A Escavador não assina o \
                    corpo por HMAC hoje; se passar a assinar, a assinatura vira a validação \
                    primária e este token continua como camada extra. \
                    Sempre responde 200 quando o token é válido: o payload cru fica registrado \
                    para auditoria e um erro de processamento não deve disparar as 10 \
                    retentativas automáticas da Escavador.
                    """
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Callback recebido e registrado"),
            @ApiResponse(responseCode = "403", description = "Token do callback ausente ou inválido")
    })
    @PostMapping("/callback")
    public void receber(@RequestBody String rawBody,
                        @RequestHeader(value = "Authorization", required = false) String authorization,
                        @RequestParam(value = "token", required = false) String token) {
        escavadorCallbackService.receber(rawBody, authorization, token);
    }
}

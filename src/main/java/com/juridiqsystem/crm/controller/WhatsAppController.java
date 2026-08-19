package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.MensagemRequest;
import com.juridiqsystem.crm.model.dtos.WhatsAppSendResultDTO;
import com.juridiqsystem.crm.model.dtos.WhatsAppSendTemplateRequestDTO;
import com.juridiqsystem.crm.service.WhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "WhatsApp", description = "Envio e recebimento de mensagens WhatsApp via Meta Cloud API.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/whatsapp")
public class WhatsAppController {

    @Autowired
    private WhatsAppService whatsAppService;

    @Operation(
            summary = "Enviar mensagem via WhatsApp (Meta Cloud API)",
            description = """
                    Requer JWT (diferente do /webhook, este endpoint é chamado pelo frontend). \
                    Envia uma mensagem de texto (e, opcionalmente, mídia via `media`, uma URL \
                    pública) para o número informado em `to`, usando a integração Meta conectada \
                    da empresa autenticada. Retorna um DTO interno — nunca o payload cru da Graph API.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagem enviada com sucesso"),
            @ApiResponse(responseCode = "409", description = "Empresa sem integração WhatsApp conectada"),
            @ApiResponse(responseCode = "502", description = "Falha ao enviar via Graph API")
    })
    @PostMapping("/send")
    public WhatsAppSendResultDTO sendMessage(@Parameter(description = "Destinatário (to), texto (message) e URL de mídia opcional (media)", required = true) @RequestBody MensagemRequest mensagemRequest) {
        return whatsAppService.sendWhatsAppMessage(mensagemRequest);
    }

    @Operation(
            summary = "Enviar template de mensagem WhatsApp aprovado",
            description = "Requer JWT. Necessário para iniciar conversa fora da janela de 24h (regra da Meta)."
    )
    @PostMapping("/send-template")
    public WhatsAppSendResultDTO sendTemplate(@RequestBody @Valid WhatsAppSendTemplateRequestDTO request) {
        return whatsAppService.sendTemplateMessage(request);
    }

    @Operation(
            summary = "Verificação do webhook (uso exclusivo da Meta)",
            description = "Endpoint público chamado uma única vez pela Meta ao cadastrar a URL de callback no painel do App, para confirmar propriedade do endpoint."
    )
    @SecurityRequirements
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {
        return ResponseEntity.ok(whatsAppService.verifyWebhook(mode, verifyToken, challenge));
    }

    @Operation(
            summary = "Webhook de recebimento de eventos WhatsApp (uso exclusivo da Meta)",
            description = """
                    Endpoint PÚBLICO (não exige JWT) — chamado diretamente pela infraestrutura da \
                    Meta quando um participante envia uma mensagem WhatsApp ou quando o status de \
                    uma mensagem enviada por nós muda (sent/delivered/read/failed). A autenticidade \
                    é validada através da assinatura enviada no header `X-Hub-Signature-256` \
                    (HMAC-SHA256 com o App Secret do SaaS). Eventos já processados (mesmo wamid) \
                    são ignorados para evitar duplicidade.
                    """
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento processado (ou ignorado, se já processado anteriormente)"),
            @ApiResponse(responseCode = "403", description = "Assinatura Meta ausente ou inválida")
    })
    @PostMapping("/webhook")
    public void receiveWebhook(@RequestBody String rawBody,
                                @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        whatsAppService.receiveWebhookEvent(rawBody, signature);
    }
}

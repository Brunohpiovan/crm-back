package com.juridiqsystem.crm.controller;
import com.juridiqsystem.crm.model.MensagemRequest;
import com.juridiqsystem.crm.service.WhatsAppService;
import com.twilio.rest.api.v2010.account.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "WhatsApp", description = "Envio e recebimento de mensagens WhatsApp via Twilio.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/whatsapp")
public class WhatsAppController {

    @Autowired
    private WhatsAppService whatsAppService;

    @Operation(
            summary = "Enviar mensagem via WhatsApp (Twilio)",
            description = """
                    Requer JWT (diferente do /webhook, este endpoint É chamado pelo frontend \
                    Angular). Envia uma mensagem de texto (e, opcionalmente, mídia via `media`, \
                    uma URL) para o número informado em `to`, usando a conta Twilio configurada. \
                    O número de destino é normalizado automaticamente para o formato \
                    "whatsapp:+55DDDNNNNNNNN" esperado pela API da Twilio. Retorna o objeto de \
                    mensagem criado pelo SDK da Twilio (com sid, status, etc.).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagem enviada com sucesso; retorna o objeto Message do SDK da Twilio"),
            @ApiResponse(responseCode = "400", description = "Falha ao enviar (número/parâmetros inválidos ou erro retornado pela API da Twilio)")
    })
    @PostMapping("/send")
    public Message sendMessage(@Parameter(description = "Destinatário (to), texto (message) e URL de mídia opcional (media)", required = true) @RequestBody MensagemRequest mensagemRequest) {
        return whatsAppService.sendWhatsAppMessage(mensagemRequest);
    }

    @Operation(
            summary = "Webhook de recebimento de mensagens WhatsApp (uso exclusivo da Twilio)",
            description = """
                    Endpoint PÚBLICO (não exige JWT) — é chamado diretamente pela infraestrutura \
                    da Twilio quando um participante envia uma mensagem WhatsApp, e não pelo \
                    frontend Angular. A autenticidade da requisição é validada "por trás dos \
                    panos" através da assinatura enviada no header `X-Twilio-Signature` \
                    (validada com o auth token da conta Twilio via `RequestValidator`); \
                    requisições sem assinatura válida são rejeitadas com 403. Eventos já \
                    processados (mesmo `MessageSid`) são ignorados para evitar duplicidade. \
                    Suporta texto, imagem, áudio e documentos (PDF/Word) como anexo. Ao \
                    processar, persiste a mensagem e publica no WebSocket em \
                    `/topic/messages/{protocoloId}` (se houver protocolo ABERTO para o remetente) \
                    ou `/topic/messages/public` (mensagem sem protocolo associado ainda).
                    """
    )
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento processado (ou ignorado, se já processado anteriormente)"),
            @ApiResponse(responseCode = "403", description = "Assinatura Twilio ausente ou inválida"),
            @ApiResponse(responseCode = "502", description = "Falha ao baixar ou reenviar mídia (imagem/áudio/documento) recebida da Twilio")
    })
    @PostMapping("/webhook")
    public void receiveWhatsAppMessage(@Parameter(description = "Parâmetros do formulário enviado pela Twilio (From, Body, ProfileName, MediaUrl0, MediaContentType0, MessageSid, etc.)") @RequestParam Map<String, String> params,
                                        @Parameter(description = "Assinatura HMAC da requisição, usada para validar que a chamada realmente veio da Twilio") @RequestHeader(value = "X-Twilio-Signature", required = false) String twilioSignature,
                                        HttpServletRequest request) {
        whatsAppService.receiveRequest(params, request.getRequestURL().toString(), twilioSignature);
    }
}




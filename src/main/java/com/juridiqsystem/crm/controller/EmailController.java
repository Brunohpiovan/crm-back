package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.EmailRequestDTO;
import com.juridiqsystem.crm.service.auth.EmailService;
import com.resend.core.exception.ResendException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "E-mail", description = "Envio avulso de e-mails (com anexos opcionais) pelo sistema, e registro do envio no histórico.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @Operation(
            summary = "Enviar e-mail",
            description = """
                    Envia um e-mail (via Resend, remetente fixo configurado em `resend.from`) para o \
                    `destinatario` informado, com `assunto` e `corpo` (HTML) e anexos opcionais. O \
                    `id_remetente` identifica o usuário do CRM responsável pelo envio (usado apenas para \
                    registro/histórico, não altera o endereço "De:" do e-mail). Requisição \
                    multipart/form-data — cada campo é enviado como um form field separado (não é um JSON \
                    em uma única parte). O envio bem-sucedido é registrado no histórico de e-mails.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "E-mail enviado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Remetente (id_remetente) não encontrado, ou falha ao anexar algum dos arquivos enviados (resposta em texto puro, não JSON estruturado; mensagem varia conforme a causa, ex.: \"Remetente não encontrado\" ou \"Erro ao anexar arquivo: ...\")",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Remetente não encontrado"))),
            @ApiResponse(responseCode = "500", description = "Falha inesperada ao enviar o e-mail (ex.: erro de comunicação com a API do Resend)")
    })
    @PostMapping(value = "/enviar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> enviarEmail(
            @Parameter(description = "Endereço de e-mail do destinatário", required = true) @RequestParam("destinatario") String destinatario,
            @Parameter(description = "Assunto do e-mail", required = true) @RequestParam("assunto") String assunto,
            @Parameter(description = "Corpo do e-mail (HTML)", required = true) @RequestParam("corpo") String corpo,
            @Parameter(description = "Id do usuário remetente (registrado no histórico de envio)", required = true) @RequestParam("id_remetente") String idRemetente,
            @Parameter(description = "Arquivos a serem anexados ao e-mail (multipart, opcional)") @RequestParam(value = "anexos", required = false) List<MultipartFile> anexos) throws ResendException {
            EmailRequestDTO email = new EmailRequestDTO(destinatario.toLowerCase(), assunto, corpo, idRemetente, anexos);
            emailService.enviarEmail(email);
            return ResponseEntity.ok().build();
    }

}

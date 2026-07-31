package br.edu.faculdadevincit.crm_vincit.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.util.List;

@Schema(description = "Payload para envio de mensagem via WhatsApp (POST /whatsapp/send).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MensagemRequest {

    @Schema(description = "Número de telefone do destinatário. Aceita diversos formatos (com/sem DDI 55, com/sem prefixo \"whatsapp:\"); é normalizado automaticamente pelo servidor.", example = "11987654321")
    private String to;
    private String message;
    @Schema(description = "URL pública de um arquivo de mídia a ser enviado junto da mensagem (opcional)")
    private String media;
}

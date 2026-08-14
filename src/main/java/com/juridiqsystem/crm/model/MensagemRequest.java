package com.juridiqsystem.crm.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Payload para envio de mensagem via WhatsApp (POST /whatsapp/send).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MensagemRequest {

    @Schema(description = "Número de telefone do destinatário. Aceita diversos formatos (com/sem DDI 55, com/sem prefixo \"whatsapp:\"); é normalizado automaticamente pelo servidor.", example = "11987654321")
    @NotBlank(message = "Informe o destinatário")
    @Size(max = 20, message = "Número de destinatário inválido")
    @Pattern(regexp = "^[0-9+() -]+$|^whatsapp:[0-9+() -]+$", message = "Número de destinatário inválido")
    private String to;

    @Size(max = 4096, message = "A mensagem deve ter no máximo 4096 caracteres")
    private String message;

    @Schema(description = "URL pública de um arquivo de mídia a ser enviado junto da mensagem (opcional)")
    @Size(max = 2048, message = "URL de mídia inválida")
    @Pattern(regexp = "^https?://.+", message = "URL de mídia deve ser http(s)")
    private String media;
}

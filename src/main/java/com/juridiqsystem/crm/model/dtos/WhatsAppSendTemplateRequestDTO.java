package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = """
        Payload para envio de um template de mensagem WhatsApp já aprovado pela Meta (obrigatório para \
        iniciar conversa fora da janela de 24h). `bodyParams` preenche as variáveis {{1}}, {{2}}... do \
        corpo do template, na ordem declarada.
        """)
@Getter
@Setter
@NoArgsConstructor
public class WhatsAppSendTemplateRequestDTO {

    @NotBlank(message = "Informe o destinatário")
    private String to;

    @NotBlank(message = "Informe o nome do template")
    private String templateName;

    @NotBlank(message = "Informe o idioma do template")
    private String languageCode;

    private List<String> bodyParams;
}

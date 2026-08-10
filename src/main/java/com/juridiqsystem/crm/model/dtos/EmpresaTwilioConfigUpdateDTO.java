package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Payload de cadastro/edição da integração Twilio (WhatsApp) de uma empresa.")
@Getter
@Setter
@NoArgsConstructor
public class EmpresaTwilioConfigUpdateDTO {

    @NotBlank(message = "Informe o Account SID")
    private String accountSid;

    @Schema(description = "Auth Token da conta Twilio. Opcional na edição: se vazio/omitido, mantém o token já salvo — o frontend nunca recebe o token real de volta para reenviar.")
    private String authToken;

    @NotBlank(message = "Informe o número de WhatsApp da Twilio")
    private String whatsappNumber;

    @NotNull(message = "Informe se a integração está habilitada")
    private Boolean enabled;
}

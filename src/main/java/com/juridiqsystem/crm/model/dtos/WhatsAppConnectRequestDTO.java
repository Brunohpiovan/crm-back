package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = """
        Payload enviado pelo frontend ao final do fluxo de Embedded Signup da Meta. `authorizationCode` \
        vem do callback FB.login (response.authResponse.code); `wabaId`/`phoneNumberId` vêm do evento \
        postMessage disparado pelo próprio iframe de Embedded Signup (data.event === "FINISH").
        """)
@Getter
@Setter
@NoArgsConstructor
public class WhatsAppConnectRequestDTO {

    @NotBlank(message = "Código de autorização ausente")
    private String authorizationCode;

    @NotBlank(message = "WABA id ausente")
    private String wabaId;

    @NotBlank(message = "Phone number id ausente")
    private String phoneNumberId;
}

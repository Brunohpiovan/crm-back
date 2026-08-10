package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do teste de conexão com a Twilio (POST /master/empresas/{empresaId}/twilio-config/test). Nunca inclui credenciais.")
public record TwilioTestResultDTO(boolean ok, String mensagem) {
}

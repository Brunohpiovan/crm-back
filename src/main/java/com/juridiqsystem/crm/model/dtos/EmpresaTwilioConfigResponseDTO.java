package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.EmpresaTwilioConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "Configuração da integração Twilio (WhatsApp) de uma empresa. O Auth Token nunca é retornado em texto puro — só uma versão mascarada, e apenas quando já existe configuração salva.")
@Getter
public class EmpresaTwilioConfigResponseDTO {

    @Schema(description = "false quando a empresa ainda não tem nenhuma integração Twilio salva (demais campos vêm nulos nesse caso)")
    private final boolean configurado;
    private final String accountSid;
    @Schema(description = "Ex.: \"********1234\". Nunca é o Auth Token real.")
    private final String authTokenMascarado;
    private final String whatsappNumber;
    private final Boolean enabled;
    private final LocalDateTime criadoEm;
    private final LocalDateTime atualizadoEm;

    public EmpresaTwilioConfigResponseDTO(EmpresaTwilioConfig config) {
        this.configurado = config != null;
        this.accountSid = config != null ? config.getAccountSid() : null;
        this.authTokenMascarado = config != null ? mascarar(config.getAuthToken()) : null;
        this.whatsappNumber = config != null ? config.getWhatsappNumber() : null;
        this.enabled = config != null ? config.getEnabled() : null;
        this.criadoEm = config != null ? config.getCriadoEm() : null;
        this.atualizadoEm = config != null ? config.getAtualizadoEm() : null;
    }

    private static String mascarar(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            return null;
        }
        int visiveis = Math.min(4, authToken.length());
        return "*".repeat(8) + authToken.substring(authToken.length() - visiveis);
    }
}

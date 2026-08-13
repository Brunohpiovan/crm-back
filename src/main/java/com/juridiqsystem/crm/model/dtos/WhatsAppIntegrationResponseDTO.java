package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.WhatsAppIntegration;
import com.juridiqsystem.crm.model.enums.WhatsAppIntegrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "Status da integração WhatsApp (Meta Cloud API) de uma empresa. Nunca inclui o access token.")
@Getter
public class WhatsAppIntegrationResponseDTO {

    @Schema(description = "false quando a empresa ainda não iniciou nenhuma conexão WhatsApp (demais campos vêm nulos nesse caso)")
    private final boolean configurado;
    private final WhatsAppIntegrationStatus status;
    private final String wabaId;
    private final String phoneNumberId;
    private final String displayPhoneNumber;
    private final String verifiedName;
    private final LocalDateTime connectedAt;
    private final LocalDateTime disconnectedAt;

    public WhatsAppIntegrationResponseDTO(WhatsAppIntegration integration) {
        this.configurado = integration != null;
        this.status = integration != null ? integration.getStatus() : WhatsAppIntegrationStatus.NOT_CONNECTED;
        this.wabaId = integration != null ? integration.getWabaId() : null;
        this.phoneNumberId = integration != null ? integration.getPhoneNumberId() : null;
        this.displayPhoneNumber = integration != null ? integration.getDisplayPhoneNumber() : null;
        this.verifiedName = integration != null ? integration.getVerifiedName() : null;
        this.connectedAt = integration != null ? integration.getConnectedAt() : null;
        this.disconnectedAt = integration != null ? integration.getDisconnectedAt() : null;
    }
}

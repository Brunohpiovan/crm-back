package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Payload publicado no tópico WebSocket /topic/protocolo/aberto/{adminId} quando um protocolo é encaminhado para um novo administrador (PUT /protocolos/encaminhar).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloNotificacao2DTO {
    private ProtocoloMoveDTO protocolo;
    private ParticipanteDTO participante;
}

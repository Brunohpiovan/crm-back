package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Payload publicado no tópico WebSocket /topic/protocolo/novo quando um novo protocolo é aberto (POST /protocolos), notificando os clientes conectados sobre o novo par admin/participante.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloNotificacaoDTO {
    @Schema(description = "Id do Participante do protocolo recém-aberto")
    private String participanteId;
    @Schema(description = "Id do Usuario (administrador) do protocolo recém-aberto")
    private String adminId;
}

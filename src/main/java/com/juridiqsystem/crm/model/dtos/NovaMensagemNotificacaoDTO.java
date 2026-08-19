package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento leve publicado em /topic/empresa/{empresaId}/notificacoes/{adminPublicId} quando chega uma mensagem nova de um cliente num protocolo já atribuído a um administrador. Usado pelo frontend para tocar a notificação sonora mesmo com a conversa fechada ou em outra página do sistema.")
public record NovaMensagemNotificacaoDTO(
        String protocoloId,
        String participanteId,
        String participanteNome,
        String preview
) {
}

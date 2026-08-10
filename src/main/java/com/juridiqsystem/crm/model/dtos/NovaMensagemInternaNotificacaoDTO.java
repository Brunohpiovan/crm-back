package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento leve publicado em /topic/empresa/{empresaId}/notificacoes-interna/{usuarioPublicId} para cada membro de um grupo de chat interno (exceto o remetente) quando chega uma mensagem nova. Usado pelo frontend para tocar a notificação sonora mesmo com a conversa fechada ou em outra página do sistema.")
public record NovaMensagemInternaNotificacaoDTO(
        String grupoId,
        String remetenteNome,
        String preview
) {
}

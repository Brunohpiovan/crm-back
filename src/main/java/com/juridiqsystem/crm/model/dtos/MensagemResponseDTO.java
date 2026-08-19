package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Mensagem;
import com.juridiqsystem.crm.model.enums.MessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "Mensagem de chat (WhatsApp) retornada pelos endpoints de /messages e publicada nos eventos WebSocket de mensagens recebidas.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MensagemResponseDTO {

    private String id;
    private SenderDTO sender;
    @Schema(description = "Texto da mensagem, ou a URL da mídia (imagem/áudio/documento) quando a mensagem não possui texto")
    private String conteudo;
    private LocalDateTime data_envio;
    @Schema(description = "Resumo do protocolo ao qual esta mensagem está vinculada; null quando a mensagem ainda não foi associada a nenhum protocolo (mensagem \"pública\")")
    private ProtocoloMessageDTO protocolo;
    @Schema(description = "Status de entrega (sent/delivered/read/failed) — só preenchido para mensagens enviadas por nós via WhatsApp; null em mensagens recebidas ou no histórico pré-migração Meta.")
    private MessageStatus status;

    public MensagemResponseDTO(Mensagem mensagem){
        this.id = mensagem.getPublicId();
        this.sender = new SenderDTO(mensagem.getSender());
        this.conteudo = mensagem.getConteudo();
        this.data_envio = mensagem.getData_envio();
        this.protocolo = mensagem.getProtocolo() != null ? new ProtocoloMessageDTO(mensagem.getProtocolo()) : null;
        this.status = mensagem.getStatus();
    }
}

package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.ChatGrupo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "Dados resumidos de um grupo de chat, usados em listagens e nas notificações WebSocket de criação/atualização de grupo.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatGrupoResponseDTO {

    private String id;
    private String nome;
    @Schema(description = "URL pública do avatar do grupo no S3 (ou o avatar padrão, se o grupo não tiver um customizado).")
    private String avatarUrl;
    @Schema(description = "URL pública da imagem de fundo do grupo no S3, se houver.")
    private String imagemFundoUrl;
    @Schema(description = "Indica se o grupo é privado (conversa 1-a-1) ou público.")
    private Boolean privado;
    @Schema(description = "Conteúdo da última mensagem enviada neste grupo. Nulo se o grupo ainda não tem mensagens.")
    private String lastMessage;
    @Schema(description = "Data/hora de envio de `lastMessage`. Nulo se `lastMessage` for nulo.")
    private LocalDateTime lastMessageAt;


    /**
     * Mantido explicitamente (não é mais o gerado por @AllArgsConstructor, que agora tem 7
     * parâmetros) porque ChatGrupoService monta este DTO por campo (sem lastMessage/lastMessageAt,
     * preenchidos depois via setter) em getGrupoByUsuarioAndPublic.
     */
    public ChatGrupoResponseDTO(String id, String nome, String avatarUrl, String imagemFundoUrl, Boolean privado) {
        this.id = id;
        this.nome = nome;
        this.avatarUrl = avatarUrl;
        this.imagemFundoUrl = imagemFundoUrl;
        this.privado = privado;
    }

    public ChatGrupoResponseDTO(ChatGrupo chatGrupo){
        this.id = chatGrupo.getPublicId();;
        this.nome = chatGrupo.getNome();;
        this.avatarUrl = chatGrupo.getAvatarUrl();;
        this.imagemFundoUrl = chatGrupo.getImagemFundoUrl();;
        this.privado = chatGrupo.getPrivado();;

    }
}

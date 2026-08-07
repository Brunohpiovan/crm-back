package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.ChatGrupo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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


    public ChatGrupoResponseDTO(ChatGrupo chatGrupo){
        this.id = chatGrupo.getPublicId();;
        this.nome = chatGrupo.getNome();;
        this.avatarUrl = chatGrupo.getAvatarUrl();;
        this.imagemFundoUrl = chatGrupo.getImagemFundoUrl();;
        this.privado = chatGrupo.getPrivado();;

    }
}

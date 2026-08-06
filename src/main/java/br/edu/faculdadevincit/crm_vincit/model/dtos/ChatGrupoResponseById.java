package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.ChatGrupo;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Dados completos de um grupo de chat, incluindo os ids de todos os usuários participantes.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatGrupoResponseById {
    private String id;
    private String nome;
    @Schema(description = "URL pública do avatar do grupo no S3 (ou o avatar padrão, se o grupo não tiver um customizado).")
    private String avatarUrl;
    @Schema(description = "URL pública da imagem de fundo do grupo no S3, se houver.")
    private String imagemFundoUrl;
    @Schema(description = "Ids de todos os usuários participantes do grupo.")
    private List<String> usuarios;

    public ChatGrupoResponseById(ChatGrupo chatGrupo){
        this.id = chatGrupo.getPublicId();;
        this.nome = chatGrupo.getNome();;
        this.avatarUrl = chatGrupo.getAvatarUrl();;
        this.imagemFundoUrl = chatGrupo.getImagemFundoUrl();
        this.usuarios = new ArrayList<>();
        for (Usuario usuario : chatGrupo.getUsuarios()) {
            this.usuarios.add(usuario.getPublicId());
        }
    }
}

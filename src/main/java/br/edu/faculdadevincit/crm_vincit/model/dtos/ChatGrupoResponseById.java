package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.ChatGrupo;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatGrupoResponseById {
    private Long id;
    private String nome;
    private String avatarUrl;
    private String imagemFundoUrl;
    private List<Long> usuarios;

    public ChatGrupoResponseById(ChatGrupo chatGrupo){
        this.id = chatGrupo.getId();;
        this.nome = chatGrupo.getNome();;
        this.avatarUrl = chatGrupo.getAvatarUrl();;
        this.imagemFundoUrl = chatGrupo.getImagemFundoUrl();
        this.usuarios = new ArrayList<>();
        for (Usuario usuario : chatGrupo.getUsuarios()) {
            this.usuarios.add(usuario.getId());
        }
    }
}

package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.ChatGrupo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatGrupoResponseDTO {

    private Long id;
    private String nome;
    private String avatarUrl;
    private String imagemFundoUrl;
    private Boolean privado;


    public ChatGrupoResponseDTO(ChatGrupo chatGrupo){
        this.id = chatGrupo.getId();;
        this.nome = chatGrupo.getNome();;
        this.avatarUrl = chatGrupo.getAvatarUrl();;
        this.imagemFundoUrl = chatGrupo.getImagemFundoUrl();;
        this.privado = chatGrupo.getPrivado();;

    }
}

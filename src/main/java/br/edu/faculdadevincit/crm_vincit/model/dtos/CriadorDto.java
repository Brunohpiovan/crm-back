package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Resumo de usuário (id, nome, email, celular, foto) usado para seleção de \"criador\" em outras entidades do CRM (ex.: funil, oportunidade).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CriadorDto {
    private Long id;
    private String nome;
    private String email;
    private String celular;
    private String urlPicture;

    public CriadorDto(Usuario usuario){
        this.id = usuario.getId();
        this.nome=usuario.getNome();
        this.email = usuario.getLogin();
        this.celular = usuario.getCelular();
        this.urlPicture = usuario.getUrlPicture();
    }
}

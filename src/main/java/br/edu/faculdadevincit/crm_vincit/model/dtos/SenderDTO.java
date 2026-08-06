package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Resumo do remetente (id, nome, foto) de uma mensagem, embutido em MensagemResponseDTO.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SenderDTO {

    private String id;
    private String nome;
    private String urlPicture;

    public SenderDTO(Participante usuario){
        this.id = usuario.getPublicId();
        this.nome = usuario.getNome();
        this.urlPicture = usuario.getUrlPicture();
    }
}

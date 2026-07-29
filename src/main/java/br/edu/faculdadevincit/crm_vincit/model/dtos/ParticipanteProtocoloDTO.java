package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.TipoParticipante;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipanteProtocoloDTO {

    private Long id;
    private String urlPicture;
    private String nome;
    private String login;
    private String celular;
    private TipoParticipante tipoParticipante;

    public ParticipanteProtocoloDTO(Participante participante) {
        this.id = participante.getId();
        this.nome = participante.getNome();

    }
}

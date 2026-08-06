package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.TipoParticipante;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * NOTA: apesar de declarar os mesmos campos que ParticipanteDTO, o construtor
 * a partir de Participante só preenche 'id' e 'nome' — os demais campos ficam
 * nulos/default nesta classe quando usada dentro de ProtocoloMoveDTO.
 */
@Schema(description = "Participante embutido em ProtocoloMoveDTO. Observação: apenas 'id' e 'nome' são efetivamente preenchidos pelo construtor a partir de Participante; os demais campos ficam nulos.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipanteProtocoloDTO {

    private String id;
    private String urlPicture;
    private String nome;
    private String login;
    private String celular;
    @Schema(description = "Tipo do participante: FUNCIONARIO (usuário interno do CRM) ou PARTICIPANTE (contato externo, ex.: cliente via WhatsApp)")
    private TipoParticipante tipoParticipante;

    public ParticipanteProtocoloDTO(Participante participante) {
        this.id = participante.getPublicId();
        this.nome = participante.getNome();

    }
}

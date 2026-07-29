package br.edu.faculdadevincit.crm_vincit.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloNotificacao2DTO {
    private ProtocoloMoveDTO protocolo;
    private ParticipanteDTO participante;
}

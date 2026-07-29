package br.edu.faculdadevincit.crm_vincit.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloNotificacaoDTO {
    private Long participanteId;
    private Long adminId;
}

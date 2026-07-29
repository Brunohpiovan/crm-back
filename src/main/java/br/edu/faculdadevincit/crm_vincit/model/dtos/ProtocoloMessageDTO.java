package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloMessageDTO {
    private StatusProtocolo status;

    public ProtocoloMessageDTO(Protocolo protocolo){
        this.status = protocolo.getStatus();
    }
}

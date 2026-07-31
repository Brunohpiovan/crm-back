package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Resumo mínimo (apenas status) do protocolo ao qual uma mensagem pertence, embutido em MensagemResponseDTO.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloMessageDTO {
    @Schema(description = "Status do protocolo: ABERTO ou FECHADO")
    private StatusProtocolo status;

    public ProtocoloMessageDTO(Protocolo protocolo){
        this.status = protocolo.getStatus();
    }
}

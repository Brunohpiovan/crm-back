package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Mensagem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MensagemResponseDTO {

    private Long id;
    private SenderDTO sender;
    private String conteudo;
    private LocalDateTime data_envio;
    private ProtocoloMessageDTO protocolo;

    public MensagemResponseDTO(Mensagem mensagem){
        this.id = mensagem.getId();
        this.sender = new SenderDTO(mensagem.getSender());
        this.conteudo = mensagem.getConteudo();
        this.data_envio = mensagem.getData_envio();
        this.protocolo = mensagem.getProtocolo() != null ? new ProtocoloMessageDTO(mensagem.getProtocolo()) : null;
    }
}

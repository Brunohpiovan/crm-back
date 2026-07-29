package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloMoveDTO {

    private Long id;
    private UsuarioAdminDTO admin;
    private String adminAnterior;
    private ParticipanteProtocoloDTO participante;
    private StatusProtocolo status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEncerramento;

    public ProtocoloMoveDTO(Protocolo protocolo){
        this.id = protocolo.getId();;
        this.admin = new UsuarioAdminDTO(protocolo.getAdmin());
        this.adminAnterior = protocolo.getAdminAnterior() != null ? protocolo.getAdminAnterior().getNome() : null;
        this.participante = new ParticipanteProtocoloDTO(protocolo.getParticipante());
        this.status = protocolo.getStatus();
        this.dataCriacao = protocolo.getDataCriacao();
        this.dataEncerramento = protocolo.getDataEncerramento();
    }
}

package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "Representação de um protocolo de atendimento, retornada pelos endpoints de /protocolos e publicada nos eventos WebSocket relacionados.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloMoveDTO {

    private String id;
    private UsuarioAdminDTO admin;
    @Schema(description = "Nome do administrador anterior, preenchido apenas quando o protocolo foi encaminhado (PUT /protocolos/encaminhar); null caso contrário")
    private String adminAnterior;
    private ParticipanteProtocoloDTO participante;
    @Schema(description = "Status atual do protocolo: ABERTO ou FECHADO")
    private StatusProtocolo status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEncerramento;

    public ProtocoloMoveDTO(Protocolo protocolo){
        this.id = protocolo.getPublicId();
        this.admin = new UsuarioAdminDTO(protocolo.getAdmin());
        this.adminAnterior = protocolo.getAdminAnterior() != null ? protocolo.getAdminAnterior().getNome() : null;
        this.participante = new ParticipanteProtocoloDTO(protocolo.getParticipante());
        this.status = protocolo.getStatus();
        this.dataCriacao = protocolo.getDataCriacao();
        this.dataEncerramento = protocolo.getDataEncerramento();
    }
}

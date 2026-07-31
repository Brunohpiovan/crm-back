package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "Payload para abertura de um novo protocolo (POST /protocolos). Apenas id_admin e id_participante são utilizados pelo service; status, dataCriacao e dataEncerramento são definidos pelo servidor.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloDto {

    private Long id;
    @Schema(description = "Id do Usuario (administrador/atendente) que abrirá o protocolo")
    @NotNull(message = "O ID do administrador não pode ser nulo.")
    private Long id_admin;
    @Schema(description = "Id do Participante a ser atendido")
    @NotNull(message = "O ID do participante não pode ser nulo.")
    private Long id_participante;
    @Schema(description = "Status do protocolo: ABERTO ou FECHADO. Ignorado na criação (o servidor sempre cria como ABERTO).")
    @NotNull(message = "O status não pode ser nulo.")
    private StatusProtocolo status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEncerramento;
}

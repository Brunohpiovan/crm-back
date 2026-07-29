package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloDto {

    private Long id;
    @NotNull(message = "O ID do administrador não pode ser nulo.")
    private Long id_admin;
    @NotNull(message = "O ID do participante não pode ser nulo.")
    private Long id_participante;
    @NotNull(message = "O status não pode ser nulo.")
    private StatusProtocolo status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEncerramento;
}

package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.TipoParticipante;
import br.edu.faculdadevincit.crm_vincit.model.enums.Uf;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Dados necessários para criar um novo participante, enviados como corpo de POST /participante. A foto/avatar recebe o valor padrão do sistema e não é informada aqui.")
public record ParticipanteCreateRequest(
        @NotBlank(message = "Informe um nome") @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres") String nome,
        @NotBlank(message = "Informe um login") @Email(message = "O e-mail deve ser válido.") @Size(max = 150, message = "O login deve ter no maximo 255 caracteres") String login,
        @Size(max = 12, message = "O rg deve ter no maximo 12 caracteres") String rg,
        @Size(max = 14, message = "O cpf deve ter no maximo 14 caracteres") String cpf,
        @Schema(pattern = "dd/MM/yyyy") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") LocalDate dataNascimento,
        @NotBlank(message = "Informe um numero de celular") @Size(max = 15, message = "O telefone deve ter no maximo 15 caracteres") String celular,
        @Size(max = 255, message = "O endereco deve ter no maximo 255 caracteres") String endereco,
        @Size(max = 50, message = "O numero residencial deve ter no maximo 50 caracteres") String numeroResidencial,
        @Size(max = 255, message = "O complemento deve ter no maximo 255 caracteres") String complemento,
        @Size(max = 100, message = "O campo bairro deve ter no maximo 100 caracteres") String bairro,
        @Schema(description = "Sigla da unidade federativa (UF) brasileira") Uf uf,
        @Size(max = 100, message = "O campo cidade deve ter no maximo 100 caracteres") String cidade,
        String observacoes,
        @Schema(description = "Tipo do participante: FUNCIONARIO (usuário interno do CRM espelhado como participante, para chat interno) ou PARTICIPANTE (contato externo, ex.: cliente via WhatsApp)")
        @NotNull(message = "Informe o tipo do participante") TipoParticipante tipoParticipante
) {
}

package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados do cliente/participante de uma oportunidade. A resolução do cliente é feita pelo campo `celular`: se já existir um participante com o mesmo celular, ele é reaproveitado/atualizado; caso contrário, um novo participante é criado.")
public record OportunidadeClienteRequest(
        @Schema(description = "Id do participante, se já conhecido; não é usado para localizar o cliente na criação (a busca é feita por celular), mas é usado diretamente na atualização (PUT) para resolver o cliente atual")
        String id,
        @NotBlank(message = "Informe um nome") @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres") String nome,
        @Schema(description = "Login do participante, se ele também for um usuário do sistema")
        @Size(max = 150, message = "O login deve ter no maximo 150 caracteres") String login,
        @Schema(description = "Celular do cliente; usado como chave para localizar um participante já existente")
        @NotBlank(message = "Informe um celular") @Size(max = 15, message = "O celular deve ter no maximo 15 caracteres") String celular
) {
}

package br.edu.faculdadevincit.crm_vincit.model.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipeCreateRequest(
        @NotBlank(message = "Informe um nome")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
        String nome) {
}

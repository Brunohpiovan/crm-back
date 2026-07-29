package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;

public record RegisterDTO(String login, String senha, UserRole cargo) {
}
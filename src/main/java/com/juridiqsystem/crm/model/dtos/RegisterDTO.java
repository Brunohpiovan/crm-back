package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.enums.UserRole;

public record RegisterDTO(String login, String senha, UserRole cargo) {
}
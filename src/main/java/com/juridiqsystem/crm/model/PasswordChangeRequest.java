package com.juridiqsystem.crm.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeRequest {
    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres.")
    private String password;

    @NotBlank
    @Size(min = 8, message = "A senha de confirmacao deve ter pelo menos 8 caracteres.")
    private String password2;
}
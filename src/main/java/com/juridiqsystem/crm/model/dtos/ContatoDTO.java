package com.juridiqsystem.crm.model.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContatoDTO {
    @NotBlank(message = "O nome é obrigatório e não pode estar em branco")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório e não pode estar em branco")
    @Email(message = "O e-mail informado é inválido")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
    private String email;

    @NotBlank(message = "O celular é obrigatório e não pode estar em branco")
    @Size(max = 15, message = "O celular deve ter no máximo 15 caracteres")
    private String celular;

    @NotBlank(message = "O interesse é obrigatório e não pode estar em branco")
    @Size(max = 100, message = "O campo interesse deve ter no máximo 100 caracteres")
    private String interesse;

    /**
     * Honeypot anti-bot: campo que não deve existir visualmente pro usuário real (no site
     * institucional, deve ficar oculto via CSS/aria-hidden, nunca com type="hidden" sozinho —
     * alguns bots já ignoram esse caso). Formulário legítimo sempre manda em branco/nulo; bots que
     * preenchem todo campo do form caem aqui. Detecção nunca é revelada na resposta (ver
     * ContatoService.create) — sempre 200 OK, como se o contato tivesse sido registrado.
     */
    @Size(max = 255)
    private String website;
}

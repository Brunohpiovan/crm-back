package br.edu.faculdadevincit.crm_vincit.model.dtos;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GrupoCreateDTO {

    private Long id;

    @Nullable
    private String urlPicture;

    @Nullable
    private String backgroundImageUrl;

    @NotNull(message = "O campo NOME é requerido.")
    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    private String nome;

    private List<Long> usuarios;
}

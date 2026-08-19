package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Funil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Schema(description = "Representação resumida de um funil (id e nome), usada em listagens e como corpo de criação/atualização de funil.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunilAllDTO {

    private String id;
    @NotBlank(message = "Informe um nome")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
    private String nome;

    public FunilAllDTO(Funil funil) {
        this.id = funil.getPublicId();
        this.nome = funil.getNome();

    }
}

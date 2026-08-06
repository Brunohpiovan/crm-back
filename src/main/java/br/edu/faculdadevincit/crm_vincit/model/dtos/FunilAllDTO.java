package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Funil;
import io.swagger.v3.oas.annotations.media.Schema;
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
    private String nome;

    public FunilAllDTO(Funil funil) {
        this.id = funil.getPublicId();
        this.nome = funil.getNome();

    }
}

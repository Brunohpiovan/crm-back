package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Funil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Representação resumida de um funil (id e nome), aninhada dentro de EtapaDto/EtapaUpdateDTO.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunilDtoCard {
    private Long id;
    private String nome;

    public FunilDtoCard(Funil funil) {
        this.id = funil.getId();
        this.nome = funil.getNome();
    }
}

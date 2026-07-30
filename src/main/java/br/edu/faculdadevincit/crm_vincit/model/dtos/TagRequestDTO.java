package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.Cor;
import br.edu.faculdadevincit.crm_vincit.model.enums.Pertence;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagRequestDTO {

    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    private String nome;

    private Cor cor;

    private Pertence pertence;

    private Situacao situacao;

}

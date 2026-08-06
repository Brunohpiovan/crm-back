package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.Etapa;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Representação resumida de uma etapa (id e nome), aninhada dentro de CadenciaFunilDto.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapaCadenciaDto {

    private String id;
    private String nome;

    public EtapaCadenciaDto(Etapa etapaOrigem) {
        this.id = etapaOrigem.getPublicId();
        this.nome = etapaOrigem.getNome();
    }
}

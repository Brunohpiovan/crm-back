package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Etapa;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Representação resumida de uma etapa (id e nome), usada em GET /etapa/funil/{id} para popular seletores.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapaFunilDTO {

    private String id;
    private String nome;

    public EtapaFunilDTO(Etapa etapa) {
        this.id = etapa.getPublicId();
        this.nome = etapa.getNome();

    }
}

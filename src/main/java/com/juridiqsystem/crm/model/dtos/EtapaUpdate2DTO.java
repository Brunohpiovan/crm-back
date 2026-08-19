package com.juridiqsystem.crm.model.dtos;


import com.juridiqsystem.crm.model.Etapa;
import com.juridiqsystem.crm.model.Oportunidade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapaUpdate2DTO {
    private String id;
    private String nome;

    public EtapaUpdate2DTO(Etapa etapa) {
        this.id = etapa.getPublicId();
        this.nome = etapa.getNome();
    }
}

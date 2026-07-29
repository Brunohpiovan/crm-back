package br.edu.faculdadevincit.crm_vincit.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapaUpdateDTO {

    private Long id;
    private String nome;
    private FunilDtoCard funil;
}

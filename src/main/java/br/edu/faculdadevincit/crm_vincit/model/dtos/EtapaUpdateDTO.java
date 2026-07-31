package br.edu.faculdadevincit.crm_vincit.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Corpo de PUT /etapa/{id}: novo nome da etapa e, opcionalmente, o funil para o qual ela deve ser movida.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EtapaUpdateDTO {

    @Schema(description = "Não utilizado pelo endpoint (o id da etapa vem do path variable); pode ser omitido")
    private Long id;
    private String nome;
    @Schema(description = "Funil de destino da etapa (apenas o id é considerado). Se omitido, o funil da etapa não é alterado.")
    private FunilDtoCard funil;
}

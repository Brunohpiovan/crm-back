package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.Cor;
import br.edu.faculdadevincit.crm_vincit.model.enums.Pertence;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Corpo de criação/atualização de uma tag (POST /tag e PUT /tag/{id}). Todos os campos são obrigatórios.")
@Getter
@Setter
public class TagRequestDTO {

    @NotBlank(message = "O nome e obrigatorio")
    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    private String nome;

    @Schema(description = "Cor de exibição da tag na interface")
    @NotNull(message = "A cor e obrigatoria")
    private Cor cor;

    @Schema(description = "A quem/o que a tag pertence/se aplica")
    @NotNull(message = "O pertencimento e obrigatorio")
    private Pertence pertence;

    @Schema(description = "Situação da tag: apenas tags ATIVA ficam disponíveis para serem associadas a novas oportunidades")
    @NotNull(message = "A situacao e obrigatoria")
    private Situacao situacao;

}

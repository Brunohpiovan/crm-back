package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Tag;
import com.juridiqsystem.crm.model.enums.Cor;
import com.juridiqsystem.crm.model.enums.Pertence;
import com.juridiqsystem.crm.model.enums.Situacao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Tag completa, retornada por GET /tag e GET /tag/{id}.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TagDTO {

    private String id;

    private String nome;

    @Schema(description = "Cor de exibição da tag na interface")
    private Cor cor;

    @Schema(description = "A quem/o que a tag pertence/se aplica")
    private Pertence pertence;

    @Schema(description = "Situação da tag: apenas tags ATIVA aparecem em GET /tag/ativas para serem associadas a novas oportunidades")
    private Situacao situacao;


    public TagDTO(Tag tag){
        this.id = tag.getPublicId();
        this.nome = tag.getNome();
        this.cor = tag.getCor();
        this.pertence = tag.getPertence();
        this.situacao = tag.getSituacao();
    }

}

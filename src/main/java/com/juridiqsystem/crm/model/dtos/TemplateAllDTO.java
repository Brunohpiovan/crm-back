package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.TemplateEmail;
import com.juridiqsystem.crm.model.enums.Situacao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Dados resumidos de um template de e-mail, usados na listagem de templates.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateAllDTO {
    private String id;
    private String nome;
    private String assunto;
    @Schema(description = "Situação do template: ATIVA ou INATIVA.")
    private Situacao situacao;

    public TemplateAllDTO(TemplateEmail templateEmail){
        this.id = templateEmail.getPublicId();
        this.nome = templateEmail.getNome();
        this.assunto = templateEmail.getAssunto();
        this.situacao = templateEmail.getSituacao();
    }
}

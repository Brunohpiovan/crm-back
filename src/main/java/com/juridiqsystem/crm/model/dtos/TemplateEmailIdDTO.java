package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.TemplateEmail;
import com.juridiqsystem.crm.model.enums.Situacao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Dados completos de um template de e-mail, incluindo mensagem e URLs dos anexos.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateEmailIdDTO {

    private String id;
    private String nome;
    @Schema(description = "Corpo/mensagem do template.")
    private String mensagem;
    private String assunto;
    @Schema(description = "Situação do template: ATIVA ou INATIVA.")
    private Situacao situacao;
    @Schema(description = "URLs públicas (no S3) dos arquivos anexados ao template.")
    private List<String> urlAnexo = new ArrayList<>();

    public TemplateEmailIdDTO(TemplateEmail templateEmail){
        this.id = templateEmail.getPublicId();
        this.nome = templateEmail.getNome();
        this.mensagem = templateEmail.getMensagem();
        this.assunto = templateEmail.getAssunto();
        this.situacao = templateEmail.getSituacao();
        this.urlAnexo = templateEmail.getUrlAnexo();
    }
}

package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.TemplateEmail;
import br.edu.faculdadevincit.crm_vincit.model.enums.Situacao;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateEmailIdDTO {

    private Long id;
    private String nome;
    private String mensagem;
    private String assunto;
    private Situacao situacao;
    private List<String> urlAnexo = new ArrayList<>();

    public TemplateEmailIdDTO(TemplateEmail templateEmail){
        this.id = templateEmail.getId();
        this.nome = templateEmail.getNome();
        this.mensagem = templateEmail.getMensagem();
        this.assunto = templateEmail.getAssunto();
        this.situacao = templateEmail.getSituacao();
        this.urlAnexo = templateEmail.getUrlAnexo();
    }
}

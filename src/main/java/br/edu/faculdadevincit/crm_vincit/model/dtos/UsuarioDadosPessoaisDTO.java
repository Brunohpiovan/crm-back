package br.edu.faculdadevincit.crm_vincit.model.dtos;

import br.edu.faculdadevincit.crm_vincit.model.enums.Uf;

import java.time.LocalDate;

public interface UsuarioDadosPessoaisDTO {
    String getUrlPicture();
    String getNome();
    String getLogin();
    String getSenha();
    String getSenha2();
    String getRg();
    String getCpf();
    LocalDate getDataNascimento();
    String getCelular();
    String getEndereco();
    String getNumeroResidencial();
    String getComplemento();
    String getBairro();
    Uf getUf();
    String getCidade();
    String getCep();
    String getObservacoes();
}

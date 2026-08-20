package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.enums.Uf;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Contrato comum entre UsuarioSelfUpdateDTO e UsuarioAdminUpdateDTO: os campos
 * de dados pessoais compartilhados por ambos os payloads de atualização de usuário.
 * Não é exposto diretamente como corpo de requisição/resposta (as implementações
 * concretas é que carregam a documentação Schema completa).
 */
@Schema(description = "Contrato de dados pessoais comuns às atualizações de usuário (self-update e admin-update).")
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
    String getOabNumero();
    Uf getOabUf();
}

package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.enums.Uf;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Resumo de usuário (id, nome, login, celular, cargo, bloqueado), usado em listagens (busca, administradores).")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAllDTO {

    private String id;
    private String nome;
    private String login;
    private String celular;
    @Schema(description = "Id público do cargo do usuário (referência para o seletor de cargo no formulário)")
    private String cargoId;
    @Schema(description = "Nome do cargo, já resolvido para exibição na tabela (ex.: \"Advogado\")")
    private String cargoNome;
    @Schema(description = "Se true, o cargo do usuário é o de administrador da empresa (acesso total)")
    private boolean administrador;
    @Schema(description = "Se true, o usuário é super-admin da plataforma (/master/**), independente do cargo")
    private Boolean master;
    @Schema(description = "Se true, o usuário está inativado/bloqueado (não consegue efetuar login)")
    private Boolean bloqueado;
    @Schema(description = "URL da foto/avatar do usuário")
    private String avatar;
    @Schema(description = "Número da OAB do advogado, se aplicável. Usado para pré-preencher o formulário de OAB monitorada (IntimacaoMonitoramento) ao selecionar este usuário como advogado dono.")
    private String oabNumero;
    @Schema(description = "UF da OAB do advogado, se aplicável.")
    private Uf oabUf;


    public UsuarioAllDTO(Usuario usuario){
        this.id = usuario.getPublicId();
        this.nome = usuario.getNome();
        this.login = usuario.getLogin();
        this.celular = usuario.getCelular();
        this.cargoId = usuario.getCargo() != null ? usuario.getCargo().getPublicId() : null;
        this.cargoNome = usuario.getCargo() != null ? usuario.getCargo().getNome() : null;
        this.administrador = usuario.getCargo() != null && usuario.getCargo().isAdministrador();
        this.master = Boolean.TRUE.equals(usuario.getMaster());
        this.bloqueado = usuario.getBloqueado();
        this.avatar = usuario.getUrlPicture();
        this.oabNumero = usuario.getOabNumero();
        this.oabUf = usuario.getOabUf();
    }

}

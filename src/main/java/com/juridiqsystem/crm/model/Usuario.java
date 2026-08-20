package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.dtos.UsuarioCreateDTO;
import com.juridiqsystem.crm.model.enums.Permissao;
import com.juridiqsystem.crm.model.enums.Uf;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Schema(description = "Usuário (funcionário). login e cpf são únicos por Empresa, não mais globalmente — a mesma pessoa pode existir em duas empresas diferentes.")
@Getter
@Setter
@Entity
@Table(name = "usuario", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_empresa_login", columnNames = {"empresa_id", "login"}),
        @UniqueConstraint(name = "uk_usuario_empresa_cpf", columnNames = {"empresa_id", "cpf"})
})
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements UserDetails {

    /** Prefixo das authorities derivadas de Permissao, consumidas por SecurityConfiguration. */
    public static final String AUTHORITY_PERMISSAO_PREFIX = "PERM_";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    /**
     * Tenant desta entidade. Auto-preenchido pelo Hibernate a partir do TenantContext no
     * insert, e usado automaticamente para filtrar toda leitura (ver TenantIdentifierResolver)
     * — não setar manualmente. "empresa" abaixo é só um espelho somente-leitura da mesma
     * coluna, pra navegação (usuario.getEmpresa().getNome() etc.).
     */
    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", insertable = false, updatable = false)
    private Empresa empresa;

    @Column(name = "urlPicture")
    private String urlPicture;

    @NotBlank(message = "Informe um nome")
    @Column(name = "nome", nullable = false)
    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    private String nome;

    @Email(message = "O e-mail deve ser válido.")
    @Column(name = "login", nullable = false)
    @Size(max = 150, message = "O login deve ter no maximo 255 caracteres")
    private String login;

    @NotBlank(message = "Informe uma senha")
    @Column(name = "senha",nullable = false)
    @Size(min = 8, max = 255, message = "O nome deve ter entre 6 e 254 caracteres")
    @JsonIgnore
    private String senha;

    /**
     * Incrementado no logout e na troca de senha; embutido como claim no JWT (ver TokenService) e
     * conferido a cada requisição autenticada (ver SecurityFilter). Um token com versão diferente
     * da atual do usuário é tratado como inválido — é assim que logout/troca de senha revogam
     * sessões que, de outra forma, continuariam válidas até a expiração natural do token (12h).
     */
    @Column(name = "sessao_versao", nullable = false)
    private Integer sessaoVersao = 0;

    @NotBlank(message = "Informe um rg")
    @Column(name = "rg",nullable = false)
    @Size(max = 12, message = "O rg deve ter no maximo 12 caracteres")
    private String rg;

    @NotBlank(message = "Informe um cpf")
    @Column(name = "cpf",nullable = false)
    @Size(max = 14, message = "O cpf deve ter no maximo 14 caracteres")
    private String cpf;

    @Column(name = "dataNascimento",nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate dataNascimento;

    @NotBlank(message = "Informe um numero de celular")
    @Column(name = "celular",nullable = false)
    @Size(max = 15, message = "O telefone deve ter no maximo 15 caracteres")
    private String celular;

    /**
     * Cargo da empresa a que este usuário pertence ("Advogado", "Secretário"...). EAGER
     * (padrão de @ManyToOne, explícito aqui por ser crítico): getAuthorities() depende dele e
     * roda fora de sessão JPA aberta (open-in-view=false) — inclusive entre frames STOMP, onde
     * o Usuario fica guardado nos sessionAttributes do WebSocket.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cargo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usuario_cargo"))
    private Cargo cargo;

    /**
     * Super-admin da plataforma (dono/operador do juriq-crm), não é um cargo de
     * empresa-cliente: é ortogonal a `cargo`, autenticado numa empresa interna reservada e
     * restrito a /master/**. Substitui o antigo valor de enum UserRole.MASTER.
     */
    @Schema(description = "Se true, o usuário é super-admin da plataforma (acesso exclusivo a /master/**), independente do cargo.")
    @Column(name = "master", nullable = false)
    private Boolean master = false;

    @NotBlank(message = "Informe um endereco")
    @Column(name = "endereco",nullable = false)
    @Size(max = 255, message = "O endereco deve ter no maximo 255 caracteres")
    private String endereco;

    @NotBlank(message = "Informe uma Numero residencial")
    @Column(name = "numeroResidencial",nullable = false)
    @Size(max = 50, message = "O numero residencial deve ter no maximo 50 caracteres")
    private String numeroResidencial;

    @Column(name = "complemento")
    @Size(max = 255, message = "O complemento deve ter no maximo 255 caracteres")
    private String complemento;

    @NotBlank(message = "Informe um bairro")
    @Column(name = "bairro",nullable = false)
    @Size(max = 100, message = "O campo bairro deve ter no maximo 100 caracteres")
    private String bairro;

    @Column(name = "uf",nullable = false)
    @Enumerated(EnumType.STRING)
    private Uf uf;

    @NotBlank(message = "Informe uma cidade")
    @Column(name = "cidade",nullable = false)
    @Size(max = 100, message = "O campo cidade deve ter no maximo 100 caracteres")
    private String cidade;

    @NotBlank(message = "Informe um cep")
    @Column(name = "cep", nullable = false)
    @Size(max = 9, message = "O cep deve ter no maximo 9 caracteres")
    private String cep;

    @Column(name = "observacoes",columnDefinition = "TEXT")
    @Lob
    private String observacoes;

    @ManyToMany(mappedBy = "funcionarios")
    @JsonIgnore
    private List<Funil> funisPermitidos;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "bloqueado",nullable = false)
    private Boolean bloqueado;

    @Schema(description = "Número da OAB do advogado, se aplicável. Opcional — usado para pré-preencher o vínculo advogado/OAB ao cadastrar uma OAB monitorada em IntimacaoMonitoramento.")
    @Column(name = "oab_numero", length = 20)
    private String oabNumero;

    @Schema(description = "UF da OAB do advogado, se aplicável.")
    @Column(name = "oab_uf", length = 2)
    @Enumerated(EnumType.STRING)
    private Uf oabUf;

    public Usuario(UsuarioCreateDTO usuarioDTO){
        this.urlPicture = usuarioDTO.getUrlPicture();
        this.nome = usuarioDTO.getNome();
        this.login = usuarioDTO.getLogin().toLowerCase();
        this.senha = usuarioDTO.getSenha();
        this.rg = usuarioDTO.getRg();
        this.cpf = usuarioDTO.getCpf();
        this.dataNascimento = usuarioDTO.getDataNascimento();
        this.celular = usuarioDTO.getCelular();
        this.endereco = usuarioDTO.getEndereco();
        this.numeroResidencial = usuarioDTO.getNumeroResidencial();
        this.complemento = usuarioDTO.getComplemento();
        this.bairro = usuarioDTO.getBairro();
        this.uf = usuarioDTO.getUf();
        this.cidade = usuarioDTO.getCidade();
        this.observacoes = usuarioDTO.getObservacoes();
        this.cep = usuarioDTO.getCep();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if (Boolean.TRUE.equals(this.master)) {
            // Master administra empresas/usuários via /master/**, sem acesso às telas normais
            // do CRM (chat, funil, etc.) — por isso não recebe ROLE_ADMIN nem ROLE_VENDEDOR,
            // e é rejeitado automaticamente por todas as regras de rota de empresa.
            authorities.add(new SimpleGrantedAuthority("ROLE_MASTER"));
            return authorities;
        }

        // Baseline de todo usuário de empresa: o uso do dia a dia (oportunidades, chat) não
        // depende de cargo nenhum.
        authorities.add(new SimpleGrantedAuthority("ROLE_VENDEDOR"));

        if (this.cargo == null) {
            // Defensivo: cargo_id é NOT NULL no banco, mas um NPE aqui derrubaria toda
            // requisição autenticada — melhor degradar para o acesso mínimo.
            return authorities;
        }

        if (this.cargo.isAdministrador()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            // Acesso total vem do flag, não de linhas em cargo_permissao: um admin nunca fica
            // "capado" por uma permissão faltando/corrompida na tabela.
            for (Permissao permissao : Permissao.values()) {
                authorities.add(new SimpleGrantedAuthority(AUTHORITY_PERMISSAO_PREFIX + permissao.name()));
            }
        } else {
            for (Permissao permissao : this.cargo.getPermissoes()) {
                authorities.add(new SimpleGrantedAuthority(AUTHORITY_PERMISSAO_PREFIX + permissao.name()));
            }
        }

        return authorities;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}

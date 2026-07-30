package br.edu.faculdadevincit.crm_vincit.model;

import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioCreateDTO;
import br.edu.faculdadevincit.crm_vincit.model.enums.Uf;
import com.fasterxml.jackson.annotation.JsonFormat;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "usuario")
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "urlPicture")
    private String urlPicture;

    @NotBlank(message = "Informe um nome")
    @Column(name = "nome", nullable = false)
    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    private String nome;

    @Email(message = "O e-mail deve ser válido.")
    @Column(name = "login", nullable = false, unique = true)
    @Size(max = 150, message = "O login deve ter no maximo 255 caracteres")
    private String login;

    @NotBlank(message = "Informe uma senha")
    @Column(name = "senha",nullable = false)
    @Size(min = 8, max = 255, message = "O nome deve ter entre 6 e 254 caracteres")
    @JsonIgnore
    private String senha;

    @NotBlank(message = "Informe um rg")
    @Column(name = "rg",nullable = false)
    @Size(max = 12, message = "O rg deve ter no maximo 12 caracteres")
    private String rg;

    @NotBlank(message = "Informe um cpf")
    @Column(name = "cpf",nullable = false, unique = true)
    @Size(max = 14, message = "O cpf deve ter no maximo 14 caracteres")
    private String cpf;

    @Column(name = "dataNascimento",nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate dataNascimento;

    @NotBlank(message = "Informe um numero de celular")
    @Column(name = "celular",nullable = false)
    @Size(max = 15, message = "O telefone deve ter no maximo 15 caracteres")
    private String celular;

    @Column(name = "cargo",nullable = false)
    private UserRole cargo;   //Exemplo: admin, user, maneger

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
        this.cargo = usuarioDTO.getCargo();
        this.cep = usuarioDTO.getCep();
    }

    public Usuario(String login,String senha,UserRole cargo){
        this.login = login;
        this.senha = senha;
        this.cargo = cargo;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if (this.cargo == UserRole.ADMINISTRADOR) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_VENDEDOR"));
        } else{
            authorities.add(new SimpleGrantedAuthority("ROLE_VENDEDOR"));
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

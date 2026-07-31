package br.edu.faculdadevincit.crm_vincit.model;

import br.edu.faculdadevincit.crm_vincit.model.enums.TipoParticipante;
import br.edu.faculdadevincit.crm_vincit.model.enums.Uf;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade JPA usada diretamente como corpo de requisição/resposta em
 * POST /participante e PUT /participante/{id} (ParticipanteController não
 * usa um DTO dedicado de entrada nesses dois endpoints).
 */
@Schema(description = "Participante (contato externo do CRM, ex.: cliente via WhatsApp) ou usuário interno espelhado (tipoParticipante = FUNCIONARIO). Usado como corpo de requisição em POST/PUT /participante.")
@Getter
@Setter
@Entity
@Table(name = "participante")
@NoArgsConstructor
@AllArgsConstructor
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "urlPicture")
    private String urlPicture;

    @Column(name = "nome", nullable = false)
    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    private String nome;

    @Email(message = "O e-mail deve ser válido.")
    @Column(name = "login",unique = true)
    @Size(max = 150, message = "O login deve ter no maximo 255 caracteres")
    private String login;

    @Column(name = "rg")
    @Size(max = 12, message = "O rg deve ter no maximo 12 caracteres")
    private String rg;

    @Column(name = "cpf", unique = true)
    @Size(max = 14, message = "O cpf deve ter no maximo 14 caracteres")
    private String cpf;

    @Column(name = "dataNascimento")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate dataNascimento;

    @NotBlank(message = "Informe um numero de celular")
    @Column(name = "celular",nullable = false)
    @Size(max = 15, message = "O telefone deve ter no maximo 15 caracteres")
    private String celular;

    @Column(name = "endereco")
    @Size(max = 255, message = "O endereco deve ter no maximo 255 caracteres")
    private String endereco;

    @Column(name = "numeroResidencial")
    @Size(max = 50, message = "O numero residencial deve ter no maximo 50 caracteres")
    private String numeroResidencial;

    @Column(name = "complemento")
    @Size(max = 255, message = "O complemento deve ter no maximo 255 caracteres")
    private String complemento;

    @Column(name = "bairro")
    @Size(max = 100, message = "O campo bairro deve ter no maximo 100 caracteres")
    private String bairro;

    @Schema(description = "Sigla da unidade federativa (UF) brasileira")
    @Column(name = "uf")
    @Enumerated(EnumType.STRING)
    private Uf uf;

    @Column(name = "cidade")
    @Size(max = 100, message = "O campo cidade deve ter no maximo 100 caracteres")
    private String cidade;

    @Column(name = "observacoes",columnDefinition = "TEXT")
    @Lob
    private String observacoes;

    @Schema(description = "Tipo do participante: FUNCIONARIO (usuário interno do CRM espelhado como participante, para chat interno) ou PARTICIPANTE (contato externo, ex.: cliente via WhatsApp)")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_participante", nullable = false)
    private TipoParticipante tipoParticipante;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    public Participante(Participante participante) {
        this.urlPicture = participante.getUrlPicture();
        this.nome = participante.getNome();
        this.login = participante.getLogin();
        this.rg = participante.getRg();
        this.cpf = participante.getCpf();
        this.dataNascimento = participante.getDataNascimento();
        this.celular = participante.getCelular();
        this.endereco = participante.getEndereco();
        this.numeroResidencial = participante.getNumeroResidencial();
        this.complemento = participante.getComplemento();
        this.bairro = participante.getBairro();
        this.uf = participante.getUf();
        this.cidade = participante.getCidade();
        this.observacoes = participante.getObservacoes();
        this.tipoParticipante = getTipoParticipante();
    }

    public Participante(Usuario usuario) {
        this.urlPicture = usuario.getUrlPicture();
        this.nome = usuario.getNome();
        this.login = usuario.getLogin();
        this.rg = usuario.getRg();
        this.cpf = usuario.getCpf();
        this.dataNascimento = usuario.getDataNascimento();
        this.celular = usuario.getCelular();
        this.endereco = usuario.getEndereco();
        this.numeroResidencial = usuario.getNumeroResidencial();
        this.complemento = usuario.getComplemento();
        this.bairro = usuario.getBairro();
        this.uf = usuario.getUf();
        this.cidade = usuario.getCidade();
        this.observacoes = usuario.getObservacoes();
    }
}

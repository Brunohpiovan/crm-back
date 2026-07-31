package br.edu.faculdadevincit.crm_vincit.model;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Schema(description = "Entidade Funil, usada diretamente como corpo de POST /funil (não é um DTO dedicado). Apenas o campo `nome` precisa ser informado ao criar; os demais campos são preenchidos/gerenciados pelo backend.")
@Getter
@Setter
@Entity
@Table(name = "funil")
@NoArgsConstructor
@AllArgsConstructor
public class Funil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    @Schema(description = "Etapas pertencentes a este funil (gerenciadas via /etapa, não são criadas diretamente aqui)")
    @OneToMany(mappedBy = "funil", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Etapa> etapas;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "funil_funcionarios",
            joinColumns = @JoinColumn(name = "funil_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private List<Usuario> funcionarios;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

}

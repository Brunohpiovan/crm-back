package br.edu.faculdadevincit.crm_vincit.model;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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

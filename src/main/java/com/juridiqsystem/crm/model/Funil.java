package com.juridiqsystem.crm.model;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Schema(description = "Entidade Funil (pipeline de vendas). Etapas e funcionários são geridos por endpoints próprios; para criar um funil, use FunilCreateRequest.")
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

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    private String nome;
    @Schema(description = "Etapas pertencentes a este funil (gerenciadas via /etapa, não são criadas diretamente aqui)")
    @OneToMany(mappedBy = "funil", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Etapa> etapas = new HashSet<>();
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

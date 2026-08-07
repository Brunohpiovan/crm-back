package com.juridiqsystem.crm.model;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Entidade Etapa (coluna de um funil de vendas). Para criar uma etapa, use EtapaCreateRequest.")
@Getter
@Setter
@Entity
@Table(name = "etapa")
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Etapa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @NotBlank(message = "Informe um nome")
    @Column(name = "nome", nullable = false)
    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    private String nome;
    @Schema(description = "Funil ao qual esta etapa pertence (apenas o id é considerado ao criar)")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funil_id")
    @NotNull
    private Funil funil;
    @Schema(description = "Oportunidades pertencentes a esta etapa (não são criadas diretamente aqui)")
    @OneToMany(mappedBy = "etapa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Oportunidade> oportunidades = new ArrayList<>();
    @Schema(description = "Soma do valor de todas as oportunidades da etapa; calculado/gerenciado pelo backend, não precisa ser informado ao criar")
    private BigDecimal valor_total;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}

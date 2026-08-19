package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.Cor;
import com.juridiqsystem.crm.model.enums.Pertence;
import com.juridiqsystem.crm.model.enums.Situacao;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tag", uniqueConstraints = @UniqueConstraint(name = "uk_tag_empresa_nome", columnNames = {"empresa_id", "nome"}))
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    private String nome;

    @Enumerated(EnumType.STRING)
    private Cor cor;

    @Enumerated(EnumType.STRING)
    private Pertence pertence;

    @Enumerated(EnumType.STRING)
    private Situacao situacao;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}

package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.Situacao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "template_email")
@NoArgsConstructor
@AllArgsConstructor
public class TemplateEmail {

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
    private String assunto;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    private Situacao situacao;
    @ElementCollection
    @CollectionTable(name = "template_email_anexos", joinColumns = @JoinColumn(name = "template_email_id"))
    @Column(name = "url_anexo")
    private List<String> urlAnexo = new ArrayList<>();

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

}

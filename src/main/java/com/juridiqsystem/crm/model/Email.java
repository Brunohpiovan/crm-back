package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.dtos.EmailRequestDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "email")
@NoArgsConstructor
@AllArgsConstructor
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tenant desta entidade (ver TenantIdentifierResolver) — faltava aqui desde a migration
     * multi-tenant original (única entidade de negócio sem isolamento por empresa), auto-
     * preenchido pelo Hibernate a partir do TenantContext no insert, igual toda outra entidade
     * @TenantId. Não setar manualmente.
     */
    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    private String destinatario;
    private String assunto;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String corpo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remetente_id", nullable = false)
    private Usuario remetente;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    public Email(EmailRequestDTO emailRequestDTO,Usuario remetente){
        this.destinatario = emailRequestDTO.getDestinatario();
        this.assunto = emailRequestDTO.getAssunto();
        this.corpo = emailRequestDTO.getCorpo();
        this.remetente = remetente;
    }

}

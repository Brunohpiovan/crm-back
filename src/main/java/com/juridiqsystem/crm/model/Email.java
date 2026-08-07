package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.dtos.EmailRequestDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Entity
@Table(name = "mensagem")
@NoArgsConstructor
@AllArgsConstructor
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocolo_id")
    private Protocolo protocolo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Participante sender;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String conteudo;
    private LocalDateTime data_envio;

    /** wamid da Meta — presente em mensagens enviadas por nós (resposta do envio) e recebidas (id do evento do webhook). Nulo no histórico pré-migração Twilio->Meta. */
    @Column(name = "external_message_id", unique = true, length = 128)
    private String externalMessageId;

    /** Só é atualizado para mensagens enviadas por nós; a Meta reporta sent/delivered/read/failed via webhook de status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private MessageStatus status;

    public String getDataEnvio() {
        if (this.data_envio == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return this.data_envio.format(formatter);
    }
}

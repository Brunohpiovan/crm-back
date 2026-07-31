package br.edu.faculdadevincit.crm_vincit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "whatsapp_webhook_evento")
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappWebhookEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "message_sid", nullable = false, unique = true, length = 64)
    private String messageSid;

    @Column(name = "processado_em", nullable = false)
    private LocalDateTime processadoEm;
}

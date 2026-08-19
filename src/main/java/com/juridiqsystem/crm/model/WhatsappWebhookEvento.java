package com.juridiqsystem.crm.model;

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
@Table(name = "whatsapp_webhook_evento")
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappWebhookEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    /** wamid da Meta — globalmente único por natureza, não faz sentido escopar por empresa. Antes "messageSid" (SID da Twilio). */
    @Column(name = "external_message_id", nullable = false, unique = true, length = 128)
    private String externalMessageId;

    @Column(name = "processado_em", nullable = false)
    private LocalDateTime processadoEm;
}

package br.edu.faculdadevincit.crm_vincit.model;

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

    /** SID do Twilio — globalmente único por natureza (é a própria Twilio quem garante isso, não faz sentido escopar por empresa). */
    @Column(name = "message_sid", nullable = false, unique = true, length = 64)
    private String messageSid;

    @Column(name = "processado_em", nullable = false)
    private LocalDateTime processadoEm;
}

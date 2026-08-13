package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.infra.security.crypto.EncryptedStringConverter;
import com.juridiqsystem.crm.model.enums.WhatsAppIntegrationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

/**
 * Integração WhatsApp (Meta Cloud API) de uma Empresa — substitui a antiga EmpresaTwilioConfig.
 * 1:1 por empresa (uk_whatsapp_integration_empresa). O phoneNumberId é único no sistema
 * (uk_whatsapp_integration_phone_number_id): é a partir dele que o webhook da Meta resolve para
 * qual empresa uma mensagem recebida pertence — ver
 * WhatsAppIntegrationRepository.findByPhoneNumberIdIgnoringTenant. Cada empresa é dona da própria
 * WABA/número (Embedded Signup como Tech Provider) — nunca existe uma WABA única compartilhada
 * entre empresas.
 */
@Getter
@Setter
@Entity
@Table(name = "whatsapp_integration")
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "waba_id", length = 64)
    private String wabaId;

    @Column(name = "phone_number_id", length = 64)
    private String phoneNumberId;

    @Column(name = "business_id", length = 64)
    private String businessId;

    @Column(name = "display_phone_number", length = 32)
    private String displayPhoneNumber;

    @Column(name = "verified_name", length = 150)
    private String verifiedName;

    /** Cifrado em repouso (AES/GCM) — nunca lido/gravado em texto puro fora do converter, nunca exposto ao frontend. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "access_token_encrypted")
    private String accessToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WhatsAppIntegrationStatus status = WhatsAppIntegrationStatus.NOT_CONNECTED;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}

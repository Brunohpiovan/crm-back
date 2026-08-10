package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.infra.security.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

/**
 * Integração Twilio (WhatsApp) de uma Empresa — substitui a antiga configuração global única
 * (TwilioConfig/@Value). 1:1 por empresa (uk_empresa_twilio_config_empresa). O número WhatsApp é
 * único no sistema (uk_empresa_twilio_config_whatsapp_number): é a partir dele que o webhook da
 * Twilio resolve para qual empresa uma mensagem recebida pertence — ver
 * EmpresaTwilioConfigRepository.findByWhatsappNumberIgnoringTenant.
 */
@Getter
@Setter
@Entity
@Table(name = "empresa_twilio_config")
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaTwilioConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @TenantId
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "account_sid", nullable = false, length = 64)
    private String accountSid;

    /** Cifrado em repouso (AES/GCM) — nunca lido/gravado em texto puro fora do converter. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "auth_token", nullable = false)
    private String authToken;

    @Column(name = "whatsapp_number", nullable = false, length = 32)
    private String whatsappNumber;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}

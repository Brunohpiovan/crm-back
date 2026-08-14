package com.juridiqsystem.crm.infra.whatsapp;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.WhatsAppIntegration;
import com.juridiqsystem.crm.model.enums.WhatsAppIntegrationStatus;
import com.juridiqsystem.crm.repository.WhatsAppIntegrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Atalho de desenvolvimento (ver docs/whatsapp/META_SETUP_SANDBOX.md, passo 6): grava uma
 * WhatsAppIntegration direto no banco local, sem passar pelo Embedded Signup, usando o
 * EncryptedStringConverter da própria entidade para cifrar o access token corretamente (não dá
 * para colar o token em texto puro via SQL manual). Só roda com o profile "dev" e apenas quando
 * whatsapp.seed.empresa-id está preenchido — nunca ativo em produção.
 */
@Component
@Profile("dev")
@Slf4j
public class WhatsAppIntegrationDevSeeder implements CommandLineRunner {

    @Autowired
    private WhatsAppIntegrationRepository repository;

    @Value("${whatsapp.seed.empresa-id:}")
    private String empresaIdRaw;

    @Value("${whatsapp.seed.waba-id:}")
    private String wabaId;

    @Value("${whatsapp.seed.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.seed.access-token:}")
    private String accessToken;

    @Override
    public void run(String... args) {
        if (empresaIdRaw == null || empresaIdRaw.isBlank()) {
            return;
        }
        Long empresaId = Long.valueOf(empresaIdRaw.trim());
        TenantContext.runAs(empresaId, () -> {
            WhatsAppIntegration integration = repository.findByEmpresaId(empresaId).orElseGet(WhatsAppIntegration::new);
            integration.setEmpresaId(empresaId);
            integration.setWabaId(wabaId);
            integration.setPhoneNumberId(phoneNumberId);
            integration.setAccessToken(accessToken);
            integration.setStatus(WhatsAppIntegrationStatus.CONNECTED);
            integration.setConnectedAt(LocalDateTime.now());
            if (integration.getCriadoEm() == null) {
                integration.setCriadoEm(LocalDateTime.now());
            }
            integration.setAtualizadoEm(LocalDateTime.now());
            repository.save(integration);
            log.info("WhatsAppIntegration (dev seed) gravada para empresaId={} phoneNumberId={}", empresaId, phoneNumberId);
            return null;
        });
    }
}

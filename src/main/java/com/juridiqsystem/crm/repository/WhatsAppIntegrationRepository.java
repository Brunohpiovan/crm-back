package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.WhatsAppIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhatsAppIntegrationRepository extends JpaRepository<WhatsAppIntegration, Long> {

    Optional<WhatsAppIntegration> findByEmpresaId(Long empresaId);

    /**
     * Nativa de propósito, mesmo motivo de EmpresaTwilioConfigRepository.findByWhatsappNumberIgnoringTenant
     * (que esta classe substitui): chamada pelo webhook público da Meta (/whatsapp/webhook) ANTES de o
     * tenant ser conhecido — é exatamente esta consulta que resolve a qual empresa uma mensagem recebida
     * pertence, a partir do phone_number_id em value.metadata.phone_number_id. Uma query JPQL normal
     * seria filtrada pelo TenantIdentifierResolver (sentinela -1) e nunca encontraria nada.
     */
    @Query(value = "SELECT * FROM whatsapp_integration WHERE phone_number_id = :phoneNumberId", nativeQuery = true)
    Optional<WhatsAppIntegration> findByPhoneNumberIdIgnoringTenant(@Param("phoneNumberId") String phoneNumberId);
}

package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.EmpresaTwilioConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaTwilioConfigRepository extends JpaRepository<EmpresaTwilioConfig, Long> {

    Optional<EmpresaTwilioConfig> findByEmpresaId(Long empresaId);

    /**
     * Nativa de propósito, mesmo motivo de UsuarioRepository.findByEmpresaCodigoAndLogin: chamada
     * pelo webhook público da Twilio (/whatsapp/webhook) ANTES de o tenant ser conhecido — é
     * exatamente esta consulta que resolve a qual empresa uma mensagem recebida pertence, a partir
     * do número de destino ("To"). Uma query JPQL normal seria filtrada pelo
     * TenantIdentifierResolver (sentinela -1) e nunca encontraria nada.
     */
    @Query(value = "SELECT * FROM empresa_twilio_config WHERE whatsapp_number = :whatsappNumber", nativeQuery = true)
    Optional<EmpresaTwilioConfig> findByWhatsappNumberIgnoringTenant(@Param("whatsappNumber") String whatsappNumber);
}

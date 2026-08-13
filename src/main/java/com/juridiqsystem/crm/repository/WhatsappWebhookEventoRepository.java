package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.WhatsappWebhookEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhatsappWebhookEventoRepository extends JpaRepository<WhatsappWebhookEvento, Long> {
    boolean existsByExternalMessageId(String externalMessageId);

    void deleteByExternalMessageId(String externalMessageId);
}

package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.WhatsappWebhookEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhatsappWebhookEventoRepository extends JpaRepository<WhatsappWebhookEvento, Long> {
    boolean existsByMessageSid(String messageSid);
}

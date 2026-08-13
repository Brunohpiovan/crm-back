package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.infra.whatsapp.MetaWhatsAppClient;
import com.juridiqsystem.crm.infra.whatsapp.dto.MetaPhoneNumberDetails;
import com.juridiqsystem.crm.infra.whatsapp.dto.MetaTokenExchangeResult;
import com.juridiqsystem.crm.model.WhatsAppIntegration;
import com.juridiqsystem.crm.model.dtos.WhatsAppConnectRequestDTO;
import com.juridiqsystem.crm.model.dtos.WhatsAppIntegrationResponseDTO;
import com.juridiqsystem.crm.model.enums.WhatsAppIntegrationStatus;
import com.juridiqsystem.crm.repository.WhatsAppIntegrationRepository;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Gerencia a integração WhatsApp (Meta Cloud API, Embedded Signup) de cada empresa — usado pela
 * tela de Configurações (/empresa/whatsapp-integration, ROLE_ADMIN), onde cada empresa conecta o
 * próprio número, sem depender do usuário master. Substitui EmpresaTwilioConfigService. Todo
 * método recebe explicitamente o empresaId e usa TenantContext.runAs — mesmo quando o chamador já
 * está no tenant certo (autoatendimento), isso mantém o service reutilizável e sem depender de
 * estado ambiente implícito.
 */
@Slf4j
@Service
public class WhatsAppIntegrationService {

    @Autowired
    private WhatsAppIntegrationRepository repository;

    @Autowired
    private MetaWhatsAppClient metaWhatsAppClient;

    @Autowired
    private SecurityLogger securityLogger;

    public WhatsAppIntegrationResponseDTO buscarParaEmpresa(Long empresaId) {
        return TenantContext.runAs(empresaId,
                () -> new WhatsAppIntegrationResponseDTO(repository.findByEmpresaId(empresaId).orElse(null)));
    }

    /**
     * Etapa final do Embedded Signup: troca o código de autorização por um token de acesso de
     * negócio, busca os dados públicos do número (display_phone_number/verified_name) e inscreve
     * o App do SaaS para receber webhooks dessa WABA — sem essa inscrição, nenhuma mensagem
     * recebida chega no nosso /whatsapp/webhook.
     */
    public WhatsAppIntegrationResponseDTO conectar(Long empresaId, WhatsAppConnectRequestDTO dto) {
        return TenantContext.runAs(empresaId, () -> {
            WhatsAppIntegration integration = repository.findByEmpresaId(empresaId).orElseGet(WhatsAppIntegration::new);
            integration.setEmpresaId(empresaId);
            integration.setStatus(WhatsAppIntegrationStatus.CONNECTING);
            integration.setAtualizadoEm(LocalDateTime.now());
            if (integration.getCriadoEm() == null) {
                integration.setCriadoEm(LocalDateTime.now());
            }
            repository.save(integration);

            try {
                MetaTokenExchangeResult tokenResult = metaWhatsAppClient.exchangeCodeForToken(dto.getAuthorizationCode());
                MetaPhoneNumberDetails details = metaWhatsAppClient.fetchPhoneNumberDetails(dto.getPhoneNumberId(), tokenResult.accessToken());
                metaWhatsAppClient.subscribeAppToWaba(dto.getWabaId(), tokenResult.accessToken());

                integration.setWabaId(dto.getWabaId());
                integration.setPhoneNumberId(dto.getPhoneNumberId());
                integration.setAccessToken(tokenResult.accessToken());
                integration.setDisplayPhoneNumber(details.displayPhoneNumber());
                integration.setVerifiedName(details.verifiedName());
                integration.setStatus(WhatsAppIntegrationStatus.CONNECTED);
                integration.setConnectedAt(LocalDateTime.now());
                integration.setDisconnectedAt(null);
                integration.setAtualizadoEm(LocalDateTime.now());

                WhatsAppIntegration salvo;
                try {
                    salvo = repository.save(integration);
                } catch (DataIntegrityViolationException e) {
                    throw new ConflictException("Este número de WhatsApp já está conectado a outra empresa.");
                }

                securityLogger.log(SecurityEventType.ADMIN_ACTION,
                        "Integração WhatsApp (Meta) conectada para empresaId=" + empresaId + " (phoneNumberId=" + salvo.getPhoneNumberId() + ")",
                        currentActorLogin(), null, "/master/empresas/" + empresaId + "/whatsapp-integration");

                return new WhatsAppIntegrationResponseDTO(salvo);
            } catch (RuntimeException e) {
                integration.setStatus(WhatsAppIntegrationStatus.ERROR);
                integration.setAtualizadoEm(LocalDateTime.now());
                repository.save(integration);
                throw e;
            }
        });
    }

    /**
     * Não apaga a integração (preserva o histórico de mensagens já trocadas), só invalida o
     * token localmente e marca como desconectada — novos envios passam a ser bloqueados por
     * WhatsAppService.requireConnectedIntegration. O token na Meta não é revogado
     * automaticamente pela Graph API neste fluxo; o cliente pode revogar o acesso do App pelo
     * próprio Gerenciador de Negócios se desejar.
     */
    public WhatsAppIntegrationResponseDTO desconectar(Long empresaId) {
        return TenantContext.runAs(empresaId, () -> {
            WhatsAppIntegration integration = repository.findByEmpresaId(empresaId)
                    .orElseThrow(() -> new ConflictException("Integração WhatsApp não configurada para esta empresa."));

            integration.setAccessToken(null);
            integration.setStatus(WhatsAppIntegrationStatus.DISCONNECTED);
            integration.setDisconnectedAt(LocalDateTime.now());
            integration.setAtualizadoEm(LocalDateTime.now());
            WhatsAppIntegration salvo = repository.save(integration);

            securityLogger.log(SecurityEventType.ADMIN_ACTION,
                    "Integração WhatsApp (Meta) desconectada para empresaId=" + empresaId,
                    currentActorLogin(), null, "/master/empresas/" + empresaId + "/whatsapp-integration");

            return new WhatsAppIntegrationResponseDTO(salvo);
        });
    }

    private String currentActorLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}

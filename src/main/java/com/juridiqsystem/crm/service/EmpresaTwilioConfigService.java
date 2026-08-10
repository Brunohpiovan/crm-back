package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.model.EmpresaTwilioConfig;
import com.juridiqsystem.crm.model.dtos.EmpresaTwilioConfigResponseDTO;
import com.juridiqsystem.crm.model.dtos.EmpresaTwilioConfigUpdateDTO;
import com.juridiqsystem.crm.model.dtos.TwilioTestResultDTO;
import com.juridiqsystem.crm.repository.EmpresaTwilioConfigRepository;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Gerencia a integração Twilio (WhatsApp) de cada empresa — usado pelo painel de Administração de
 * Empresas (/master/empresas/{empresaId}/twilio-config, ROLE_MASTER). Todo método mira uma empresa
 * específica via TenantContext.runAs (mesmo padrão de UsuarioService.saveParaEmpresa/
 * findAllParaEmpresa), já que EmpresaTwilioConfig é uma entidade @TenantId como qualquer outra do
 * chat, e o usuário master pertence à empresa interna, não à empresa-cliente que está configurando.
 */
@Slf4j
@Service
public class EmpresaTwilioConfigService {

    @Autowired
    private EmpresaTwilioConfigRepository repository;

    @Autowired
    private TwilioClientProvider twilioClientProvider;

    @Autowired
    private SecurityLogger securityLogger;

    public EmpresaTwilioConfigResponseDTO buscarParaEmpresa(Long empresaId) {
        return TenantContext.runAs(empresaId,
                () -> new EmpresaTwilioConfigResponseDTO(repository.findByEmpresaId(empresaId).orElse(null)));
    }

    public EmpresaTwilioConfigResponseDTO salvarParaEmpresa(Long empresaId, EmpresaTwilioConfigUpdateDTO dto) {
        return TenantContext.runAs(empresaId, () -> {
            EmpresaTwilioConfig config = repository.findByEmpresaId(empresaId).orElseGet(EmpresaTwilioConfig::new);
            boolean novo = config.getId() == null;

            config.setEmpresaId(empresaId);
            config.setAccountSid(dto.getAccountSid().trim());
            if (dto.getAuthToken() != null && !dto.getAuthToken().isBlank()) {
                config.setAuthToken(dto.getAuthToken().trim());
            } else if (novo) {
                throw new ConflictException("Informe o Auth Token para configurar a integração Twilio pela primeira vez.");
            }
            config.setWhatsappNumber(normalizarNumero(dto.getWhatsappNumber()));
            config.setEnabled(dto.getEnabled());
            config.setAtualizadoEm(LocalDateTime.now());
            if (novo) {
                config.setCriadoEm(LocalDateTime.now());
            }

            EmpresaTwilioConfig salvo;
            try {
                salvo = repository.save(config);
            } catch (DataIntegrityViolationException e) {
                throw new ConflictException("Este número de WhatsApp já está cadastrado para outra empresa.");
            }

            // Credenciais podem ter mudado — nunca deixar o próximo envio usar um TwilioRestClient
            // antigo (com accountSid/authToken desatualizados) só porque ainda está no cache.
            twilioClientProvider.invalidate(empresaId);

            securityLogger.log(SecurityEventType.ADMIN_ACTION,
                    "Integração Twilio " + (novo ? "criada" : "editada") + " para empresaId=" + empresaId + " (accountSid=" + salvo.getAccountSid() + ")",
                    currentActorLogin(), null, "/master/empresas/" + empresaId + "/twilio-config");

            return new EmpresaTwilioConfigResponseDTO(salvo);
        });
    }

    /**
     * Chamada leve à API da Twilio (fetch da própria conta) só para validar que accountSid/
     * authToken salvos são credenciais válidas. Nunca retorna nem loga o Auth Token — só um
     * booleano e uma mensagem genérica.
     */
    public TwilioTestResultDTO testarConexaoParaEmpresa(Long empresaId) {
        return TenantContext.runAs(empresaId, () -> {
            EmpresaTwilioConfig config = repository.findByEmpresaId(empresaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Integração Twilio não configurada para esta empresa."));
            TwilioRestClient client = twilioClientProvider.getClient(config);
            try {
                Account.fetcher().fetch(client);
                log.info("Teste de conexao Twilio bem-sucedido. empresaId={}", empresaId);
                return new TwilioTestResultDTO(true, "Credenciais válidas.");
            } catch (Exception e) {
                log.warn("Teste de conexao Twilio falhou. empresaId={}", empresaId);
                return new TwilioTestResultDTO(false, "Não foi possível validar as credenciais informadas junto à Twilio.");
            }
        });
    }

    private String normalizarNumero(String whatsappNumber) {
        String numero = whatsappNumber.trim();
        return numero.startsWith("whatsapp:") ? numero.substring("whatsapp:".length()) : numero;
    }

    private String currentActorLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }
}

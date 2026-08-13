package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.infra.whatsapp.MetaWhatsAppProperties;
import com.juridiqsystem.crm.model.dtos.MetaAppConfigDTO;
import com.juridiqsystem.crm.model.dtos.WhatsAppConnectRequestDTO;
import com.juridiqsystem.crm.model.dtos.WhatsAppIntegrationResponseDTO;
import com.juridiqsystem.crm.service.WhatsAppIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Autoatendimento: cada empresa conecta o próprio WhatsApp (Meta Cloud API, Embedded Signup), sem
 * depender do usuário master. Restrito a ROLE_ADMIN (mesmo padrão de PUT /empresa) — a empresa é
 * sempre resolvida a partir do usuário autenticado (TenantContext), nunca de um id na URL.
 */
@Tag(name = "Configurações - Integração WhatsApp (Meta)", description = "Conexão do WhatsApp da própria empresa (Embedded Signup), restrito a administradores (ROLE_ADMIN). Cada empresa tem sua própria WABA/número — não existe WABA compartilhada.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/empresa/whatsapp-integration")
public class WhatsAppIntegrationController {

    @Autowired
    private WhatsAppIntegrationService whatsAppIntegrationService;

    @Autowired
    private MetaWhatsAppProperties metaProperties;

    @Operation(summary = "Configuração pública do Meta App do SaaS", description = "Requer JWT com cargo ROLE_ADMIN. Necessário para o frontend inicializar o Facebook JS SDK. Nunca inclui o App Secret.")
    @GetMapping("/meta-config")
    public MetaAppConfigDTO metaConfig() {
        return new MetaAppConfigDTO(metaProperties.getAppId(), metaProperties.getConfigId(), metaProperties.getGraphApiVersion());
    }

    @Operation(summary = "Buscar a integração WhatsApp da própria empresa", description = "Requer JWT com cargo ROLE_ADMIN. Nunca inclui o access token.")
    @GetMapping
    public WhatsAppIntegrationResponseDTO buscar() {
        return whatsAppIntegrationService.buscarParaEmpresa(TenantContext.get());
    }

    @Operation(
            summary = "Concluir a conexão WhatsApp da própria empresa (Embedded Signup)",
            description = "Requer JWT com cargo ROLE_ADMIN. Chamado pelo frontend ao final do fluxo de Embedded Signup da Meta, com o código de autorização e os ids de WABA/número recebidos do próprio widget."
    )
    @PostMapping("/connect")
    public WhatsAppIntegrationResponseDTO conectar(@RequestBody @Valid WhatsAppConnectRequestDTO dto) {
        return whatsAppIntegrationService.conectar(TenantContext.get(), dto);
    }

    @Operation(summary = "Desconectar a integração WhatsApp da própria empresa", description = "Requer JWT com cargo ROLE_ADMIN. Preserva o histórico de conversas; só bloqueia novos envios/recebimentos.")
    @PostMapping("/disconnect")
    public WhatsAppIntegrationResponseDTO desconectar() {
        return whatsAppIntegrationService.desconectar(TenantContext.get());
    }
}

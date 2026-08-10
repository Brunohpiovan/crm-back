package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.EmpresaTwilioConfigResponseDTO;
import com.juridiqsystem.crm.model.dtos.EmpresaTwilioConfigUpdateDTO;
import com.juridiqsystem.crm.model.dtos.TwilioTestResultDTO;
import com.juridiqsystem.crm.service.EmpresaService;
import com.juridiqsystem.crm.service.EmpresaTwilioConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Master - Integração Twilio", description = "Configuração da integração Twilio (WhatsApp) de uma empresa-cliente específica, restrito ao usuário master (ROLE_MASTER). Cada empresa tem sua própria conta Twilio — não existe mais configuração global.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/master/empresas/{empresaId}/twilio-config")
public class EmpresaTwilioConfigController {

    @Autowired
    private EmpresaTwilioConfigService empresaTwilioConfigService;

    @Autowired
    private EmpresaService empresaService;

    @Operation(summary = "Buscar a integração Twilio de uma empresa", description = "Requer JWT com cargo ROLE_MASTER. O Auth Token retornado vem sempre mascarado.")
    @GetMapping
    public EmpresaTwilioConfigResponseDTO buscar(@Parameter(description = "Id da empresa", required = true) @PathVariable String empresaId) {
        return empresaTwilioConfigService.buscarParaEmpresa(empresaService.resolverIdInterno(empresaId));
    }

    @Operation(
            summary = "Cadastrar ou atualizar a integração Twilio de uma empresa",
            description = """
                    Requer JWT com cargo ROLE_MASTER. Se `authToken` vier vazio/omitido numa \
                    edição, mantém o token já salvo. O número de WhatsApp precisa ser único no \
                    sistema (é ele quem identifica a empresa no webhook da Twilio).
                    """
    )
    @PutMapping
    public EmpresaTwilioConfigResponseDTO salvar(@Parameter(description = "Id da empresa", required = true) @PathVariable String empresaId,
                                                  @RequestBody @Valid EmpresaTwilioConfigUpdateDTO dto) {
        return empresaTwilioConfigService.salvarParaEmpresa(empresaService.resolverIdInterno(empresaId), dto);
    }

    @Operation(summary = "Testar a integração Twilio de uma empresa", description = "Requer JWT com cargo ROLE_MASTER. Faz uma chamada leve à API da Twilio para validar as credenciais já salvas; nunca retorna credenciais.")
    @PostMapping("/test")
    public TwilioTestResultDTO testar(@Parameter(description = "Id da empresa", required = true) @PathVariable String empresaId) {
        return empresaTwilioConfigService.testarConexaoParaEmpresa(empresaService.resolverIdInterno(empresaId));
    }
}

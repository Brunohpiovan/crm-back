package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.dtos.MonitoramentoQuotaResponse;
import com.juridiqsystem.crm.service.escavador.ProcessoMonitoramentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller dedicado à cota de monitoramento da empresa autenticada. Separado do
 * EmpresaController de propósito: a cota é do módulo Escavador, e mantê-la aqui evita que dois
 * módulos independentes disputem o mesmo arquivo.
 */
@Tag(name = "Monitoramento Quota", description = "Cota de processos monitorados simultaneamente do plano da empresa.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/empresas/atual/monitoramento")
public class EscavadorMonitoramentoQuotaController {

    @Autowired
    private ProcessoMonitoramentoService processoMonitoramentoService;

    @Operation(summary = "Consultar a cota de processos monitorados",
            description = "Requer JWT. `limite` null significa ilimitado. A empresa vem sempre do token — nunca de parâmetro da requisição.")
    @GetMapping("/quota")
    public MonitoramentoQuotaResponse obterQuota() {
        return processoMonitoramentoService.obterCotaAtual(TenantContext.get());
    }
}

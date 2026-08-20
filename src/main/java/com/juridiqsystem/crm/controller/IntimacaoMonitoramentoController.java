package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.dtos.IntimacaoMonitoramentoAtivarRequest;
import com.juridiqsystem.crm.model.dtos.IntimacaoMonitoramentoResponse;
import com.juridiqsystem.crm.model.dtos.IntimacaoSincronizacaoResponse;
import com.juridiqsystem.crm.model.dtos.MonitoramentoQuotaResponse;
import com.juridiqsystem.crm.service.escavador.IntimacaoMonitoramentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gerencia as OABs monitoradas nos Diários Oficiais da empresa (nível empresa/Settings, não por
 * processo — diferente de ProcessoMonitoramentoController). Mirror do padrão de
 * ProcessoMonitoramentoController + EscavadorMonitoramentoQuotaController.
 */
@Tag(name = "Intimação Monitoramento", description = "Liga e desliga o monitoramento contínuo de uma OAB nos Diários Oficiais (Escavador).")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/empresas/atual/intimacoes/monitoramentos")
public class IntimacaoMonitoramentoController {

    @Autowired
    private IntimacaoMonitoramentoService intimacaoMonitoramentoService;

    @Operation(summary = "Listar as OABs monitoradas da empresa",
            description = "Requer JWT. A empresa vem sempre do token — nunca de parâmetro da requisição.")
    @GetMapping
    public List<IntimacaoMonitoramentoResponse> listar() {
        return intimacaoMonitoramentoService.listar(TenantContext.get());
    }

    @Operation(summary = "Consultar a cota de OABs monitoradas",
            description = "Requer JWT. `limite` null significa ilimitado.")
    @GetMapping("/quota")
    public MonitoramentoQuotaResponse obterQuota() {
        return intimacaoMonitoramentoService.obterCotaAtual(TenantContext.get());
    }

    @Operation(summary = "Ligar o monitoramento de uma OAB",
            description = """
                    Requer JWT. Consome uma vaga da cota de OABs monitoradas do plano da empresa. \
                    `usuarioAdvogadoId` é opcional (publicId de um usuário da empresa, sem \
                    restrição de cargo — vínculo só informativo). Chamar de novo para uma OAB já \
                    ativa não consome outra vaga: só atualiza o advogado vinculado.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monitoramento ligado"),
            @ApiResponse(responseCode = "409", description = "Limite de OABs monitoradas do plano atingido")
    })
    @PostMapping
    public IntimacaoMonitoramentoResponse ativar(@RequestBody @Valid IntimacaoMonitoramentoAtivarRequest request) {
        return intimacaoMonitoramentoService.ativar(request.oabNumero(), request.oabUf(), request.usuarioAdvogadoId());
    }

    @Operation(summary = "Desligar o monitoramento de uma OAB",
            description = "Requer JWT. Remove a assinatura na Escavador e libera a vaga na cota do plano.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monitoramento desligado (resposta sem corpo)"),
            @ApiResponse(responseCode = "404", description = "OAB não encontrada ou não monitorada"),
            @ApiResponse(responseCode = "502", description = "Falha ao remover a assinatura na Escavador — tente novamente")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@Parameter(description = "Id da OAB monitorada", required = true) @PathVariable Long id) {
        intimacaoMonitoramentoService.desativar(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Sincronizar aparições desta OAB",
            description = """
                    Requer JWT. Rede de segurança para quando o callback de monitoramento falha ou \
                    não chega (webhook fora do ar, URL pública indisponível): busca direto na \
                    Escavador (grátis) as publicações já detectadas para esta OAB e registra as que \
                    ainda não estão no CRM. Idempotente — pode ser chamado quantas vezes quiser, só \
                    intimações novas são criadas.
                    """)
    @PostMapping("/{id}/sincronizar")
    public IntimacaoSincronizacaoResponse sincronizar(@Parameter(description = "Id da OAB monitorada", required = true) @PathVariable Long id) {
        return new IntimacaoSincronizacaoResponse(intimacaoMonitoramentoService.sincronizarAparicoes(id));
    }
}

package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.ProcessoMonitoramentoAtivarRequest;
import com.juridiqsystem.crm.model.dtos.ProcessoMonitoramentoDetailResponse;
import com.juridiqsystem.crm.service.escavador.ProcessoMonitoramentoService;
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

@Tag(name = "Processo Monitoramento", description = "Liga e desliga o monitoramento contínuo de um processo nos tribunais e diários oficiais (Escavador).")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/processos/{publicId}/monitoramento")
public class ProcessoMonitoramentoController {

    @Autowired
    private ProcessoMonitoramentoService processoMonitoramentoService;

    @Operation(summary = "Consultar o monitoramento do processo",
            description = "Requer JWT. Devolve `ativo: false` quando o processo nunca foi monitorado ou o monitoramento está desligado.")
    @GetMapping
    public ProcessoMonitoramentoDetailResponse obter(
            @Parameter(description = "publicId do processo", required = true) @PathVariable String publicId) {
        return processoMonitoramentoService.obter(publicId);
    }

    @Operation(summary = "Ligar o monitoramento do processo",
            description = """
                    Requer JWT. Consome uma vaga da cota de processos monitorados do plano da \
                    empresa. Chamar de novo com a mesma configuração é idempotente (não consome \
                    outra vaga nem cria outra assinatura); com configuração diferente, a \
                    assinatura é recriada na Escavador com os novos parâmetros.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monitoramento ligado"),
            @ApiResponse(responseCode = "404", description = "Processo não encontrado"),
            @ApiResponse(responseCode = "409", description = "Limite de processos monitorados do plano atingido")
    })
    @PostMapping
    public ProcessoMonitoramentoDetailResponse ativar(
            @Parameter(description = "publicId do processo", required = true) @PathVariable String publicId,
            @RequestBody @Valid ProcessoMonitoramentoAtivarRequest request) {
        return processoMonitoramentoService.ativar(publicId, request.frequencia(), Boolean.TRUE.equals(request.comDocumentos()));
    }

    @Operation(summary = "Desligar o monitoramento do processo",
            description = "Requer JWT. Remove a assinatura na Escavador e libera a vaga na cota do plano.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monitoramento desligado (resposta sem corpo)"),
            @ApiResponse(responseCode = "404", description = "Processo não encontrado ou não monitorado"),
            @ApiResponse(responseCode = "502", description = "Falha ao remover a assinatura na Escavador — tente novamente")
    })
    @DeleteMapping
    public ResponseEntity<Void> desativar(
            @Parameter(description = "publicId do processo", required = true) @PathVariable String publicId) {
        processoMonitoramentoService.desativar(publicId);
        return ResponseEntity.ok().build();
    }
}

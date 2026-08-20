package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.IntimacaoResponse;
import com.juridiqsystem.crm.model.dtos.PageResponse;
import com.juridiqsystem.crm.service.IntimacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publicações em Diário Oficial encontradas para as OABs monitoradas da empresa (nível empresa,
 * mesmo racional de IntimacaoMonitoramentoController).
 */
@Tag(name = "Intimações", description = "Publicações em Diário Oficial encontradas para as OABs monitoradas da empresa.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/empresas/atual/intimacoes")
public class IntimacaoController {

    @Autowired
    private IntimacaoService intimacaoService;

    @Operation(summary = "Listar intimações (paginado)",
            description = """
                    Requer JWT. `lida` filtra por lida/não-lida (omitido = todas); \
                    `usuarioAdvogadoId` filtra pelo advogado dono da OAB que gerou a intimação \
                    (publicId de um usuário da empresa, resolvido via join até \
                    IntimacaoMonitoramento — não duplicado na própria tabela de intimações). \
                    Ordenado por mais recente primeiro.
                    """)
    @GetMapping
    public PageResponse<IntimacaoResponse> listar(
            @Parameter(description = "Filtra por lida (true) ou não lida (false); omitido lista todas") @RequestParam(required = false) Boolean lida,
            @Parameter(description = "publicId do advogado (Usuario) dono da OAB monitorada") @RequestParam(required = false) String usuarioAdvogadoId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return intimacaoService.listar(lida, usuarioAdvogadoId, pageable);
    }

    @Operation(summary = "Marcar uma intimação como lida", description = "Requer JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Intimação marcada como lida (resposta sem corpo)"),
            @ApiResponse(responseCode = "404", description = "Intimação não encontrada")
    })
    @PatchMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@Parameter(description = "Id da intimação", required = true) @PathVariable Long id) {
        intimacaoService.marcarComoLida(id);
        return ResponseEntity.ok().build();
    }
}

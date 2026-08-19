package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.OportunidadeProcessoResponse;
import com.juridiqsystem.crm.model.dtos.ProcessoVincularRequest;
import com.juridiqsystem.crm.service.OportunidadeProcessoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Oportunidade Processo", description = "Vínculo entre Oportunidade e Processo judicial (Escavador).")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/oportunidades/{publicId}/processos")
public class OportunidadeProcessoController {

    private final OportunidadeProcessoService oportunidadeProcessoService;

    public OportunidadeProcessoController(OportunidadeProcessoService oportunidadeProcessoService) {
        this.oportunidadeProcessoService = oportunidadeProcessoService;
    }

    @Operation(summary = "Listar processos vinculados à oportunidade", description = "Requer JWT.")
    @GetMapping
    public List<OportunidadeProcessoResponse> listar(@PathVariable String publicId) {
        return oportunidadeProcessoService.listar(publicId);
    }

    @Operation(summary = "Vincular um processo à oportunidade",
            description = "Requer JWT. Busca o processo pelo número CNJ (criando-o localmente se ainda não existir) e cria o vínculo.")
    @PostMapping
    public ResponseEntity<OportunidadeProcessoResponse> vincular(@PathVariable String publicId, @Valid @RequestBody ProcessoVincularRequest request) {
        return ResponseEntity.ok(oportunidadeProcessoService.vincular(publicId, request.numeroCnj()));
    }

    @Operation(summary = "Desvincular um processo da oportunidade", description = "Requer JWT. Remove só o vínculo — o Processo em si não é excluído.")
    @DeleteMapping("/{processoPublicId}")
    public ResponseEntity<?> desvincular(@PathVariable String publicId, @PathVariable String processoPublicId) {
        oportunidadeProcessoService.desvincular(publicId, processoPublicId);
        return ResponseEntity.ok().build();
    }
}

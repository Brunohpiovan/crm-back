package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.dtos.AlterarSenhaDTO;
import com.juridiqsystem.crm.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Master - Conta", description = "Autoatendimento do próprio usuário master, restrito ao cargo ROLE_MASTER.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/master")
public class MasterContaController {

    @Autowired
    private UsuarioService usuarioService;

    @Operation(
            summary = "Trocar a própria senha",
            description = "Requer JWT com cargo ROLE_MASTER. Exige a senha atual; não altera nenhum outro dado do cadastro."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Senha atual incorreta, ou nova senha e confirmação não coincidem")
    })
    @PutMapping("/senha")
    public ResponseEntity<?> alterarSenha(@RequestBody @Valid AlterarSenhaDTO dto) {
        usuarioService.alterarSenhaPropria(dto);
        return ResponseEntity.ok().build();
    }
}

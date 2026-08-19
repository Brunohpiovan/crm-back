package com.juridiqsystem.crm.service.auth;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.model.PasswordResetToken;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.ApiResponse;
import com.juridiqsystem.crm.repository.PasswordResetTokenRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.exceptions.UserNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class PasswordResetService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityLogger securityLogger;

    @Transactional
    public ApiResponse changePassord(String token, String password, String password2) {
        if (!Objects.equals(password, password2)) {
            throw new IllegalArgumentException("As senhas nao coincidem");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado."));

        // Consumo atômico (UPDATE ... WHERE usado = false ...): se duas requisições chegarem
        // simultaneamente com o mesmo token válido, só uma consegue marcar usado=true e seguir
        // adiante — a outra recebe 0 linhas afetadas aqui e é rejeitada, em vez de ambas lerem
        // usado=false (SELECT) antes de qualquer uma persistir e conseguirem trocar a senha duas
        // vezes na janela da corrida.
        int atualizados = passwordResetTokenRepository.marcarComoUsadoSeValido(token, LocalDateTime.now());
        if (atualizados == 0) {
            throw new IllegalArgumentException("Token inválido ou expirado.");
        }

        Usuario user = usuarioRepository.findByIdIgnorandoTenant(resetToken.getUsuarioId())
                .orElseThrow(() -> new UserNotFoundException("Usuario nao encontrado"));

        // TenantContext ainda vazio aqui (fluxo permitAll, sem sessão autenticada) — sem setar
        // pro empresa_id real do usuário, o UPDATE do save() abaixo sairia com "AND empresa_id
        // = -1" (sentinel) no WHERE, não bateria com a linha real, e a troca de senha não
        // seria persistida.
        TenantContext.set(user.getEmpresaId());
        try {
            user.setSenha(passwordEncoder.encode(password));
            // Revoga qualquer sessão ativa com a senha antiga (ver SecurityFilter) — não há
            // token pra reemitir aqui, o usuário loga de novo com a senha nova em seguida.
            user.setSessaoVersao(user.getSessaoVersao() + 1);
            usuarioRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        securityLogger.log(SecurityEventType.PASSWORD_RESET_SUCCESS, null, user.getLogin(), null, "/reset-password");

        return new ApiResponse("Senha alterada com sucesso.");
    }
}

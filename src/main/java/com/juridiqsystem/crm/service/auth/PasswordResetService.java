package com.juridiqsystem.crm.service.auth;

import com.juridiqsystem.crm.infra.security.TenantContext;
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

    @Transactional
    public ApiResponse changePassord(String token, String password, String password2) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou expirado."));

        if (Boolean.TRUE.equals(resetToken.getUsado()) || resetToken.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token inválido ou expirado.");
        }

        if (!Objects.equals(password, password2)) {
            throw new IllegalArgumentException("As senhas nao coincidem");
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
            usuarioRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        resetToken.setUsado(true);
        passwordResetTokenRepository.save(resetToken);

        return new ApiResponse("Senha alterada com sucesso.");
    }
}

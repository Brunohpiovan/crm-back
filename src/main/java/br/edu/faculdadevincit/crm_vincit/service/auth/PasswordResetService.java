package br.edu.faculdadevincit.crm_vincit.service.auth;

import br.edu.faculdadevincit.crm_vincit.model.PasswordResetToken;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ApiResponse;
import br.edu.faculdadevincit.crm_vincit.repository.PasswordResetTokenRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.UserNotFoundException;
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

        Usuario user = usuarioRepository.findById(resetToken.getUsuarioId())
                .orElseThrow(() -> new UserNotFoundException("Usuario nao encontrado"));

        user.setSenha(passwordEncoder.encode(password));
        usuarioRepository.save(user);

        resetToken.setUsado(true);
        passwordResetTokenRepository.save(resetToken);

        return new ApiResponse("Senha alterada com sucesso.");
    }
}

package com.juridiqsystem.crm.service.auth;

import com.juridiqsystem.crm.model.PasswordResetToken;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.ApiResponse;
import com.juridiqsystem.crm.repository.PasswordResetTokenRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private PasswordResetToken tokenValido() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(1L);
        token.setToken("token-valido-123");
        token.setUsuarioId(99L);
        token.setCriadoEm(LocalDateTime.now().minusMinutes(5));
        token.setExpiraEm(LocalDateTime.now().plusMinutes(25));
        token.setUsado(false);
        return token;
    }

    @Test
    void tokenValido_alteraSenhaEMarcaTokenComoUsado() {
        PasswordResetToken token = tokenValido();
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setSenha("senha-antiga-hash");

        when(passwordResetTokenRepository.findByToken("token-valido-123")).thenReturn(Optional.of(token));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("nova-senha-hash");

        ApiResponse resposta = passwordResetService.changePassord("token-valido-123", "novaSenha123", "novaSenha123");

        assertThat(resposta.message()).isEqualTo("Senha alterada com sucesso.");
        assertThat(usuario.getSenha()).isEqualTo("nova-senha-hash");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository, times(1)).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUsado()).isTrue();
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    void tokenExpirado_naoAlteraSenhaELancaExcecao() {
        PasswordResetToken token = tokenValido();
        token.setExpiraEm(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.changePassord("token-expirado", "novaSenha123", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token inválido ou expirado.");

        verify(usuarioRepository, never()).save(any());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void tokenJaUtilizado_naoAlteraSenhaELancaExcecao() {
        PasswordResetToken token = tokenValido();
        token.setUsado(true);

        when(passwordResetTokenRepository.findByToken("token-usado")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.changePassord("token-usado", "novaSenha123", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token inválido ou expirado.");

        verify(usuarioRepository, never()).save(any());
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void tokenInexistente_lancaExcecao() {
        when(passwordResetTokenRepository.findByToken("token-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.changePassord("token-invalido", "novaSenha123", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token inválido ou expirado.");
    }
}

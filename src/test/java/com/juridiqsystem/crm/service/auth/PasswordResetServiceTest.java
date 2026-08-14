package com.juridiqsystem.crm.service.auth;

import com.juridiqsystem.crm.model.PasswordResetToken;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.ApiResponse;
import com.juridiqsystem.crm.repository.PasswordResetTokenRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    void tokenValido_alteraSenhaEConsomeTokenAtomicamente() {
        PasswordResetToken token = tokenValido();
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setSenha("senha-antiga-hash");
        usuario.setSessaoVersao(0);

        when(passwordResetTokenRepository.findByToken("token-valido-123")).thenReturn(Optional.of(token));
        // Consumo atômico (UPDATE ... WHERE usado = false ...) bem-sucedido: 1 linha afetada.
        when(passwordResetTokenRepository.marcarComoUsadoSeValido(eq("token-valido-123"), any(LocalDateTime.class)))
                .thenReturn(1);
        when(usuarioRepository.findByIdIgnorandoTenant(99L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("nova-senha-hash");

        ApiResponse resposta = passwordResetService.changePassord("token-valido-123", "novaSenha123", "novaSenha123");

        assertThat(resposta.message()).isEqualTo("Senha alterada com sucesso.");
        assertThat(usuario.getSenha()).isEqualTo("nova-senha-hash");
        assertThat(usuario.getSessaoVersao()).isEqualTo(1);

        verify(passwordResetTokenRepository, times(1)).marcarComoUsadoSeValido(eq("token-valido-123"), any(LocalDateTime.class));
        verify(usuarioRepository, times(1)).save(usuario);
    }

    /**
     * Duas requisições "simultâneas" com o mesmo token: a corrida é resolvida no UPDATE atômico
     * do banco (marcarComoUsadoSeValido), não em memória — aqui simulamos a segunda chamada
     * chegando depois que a primeira já consumiu o token (0 linhas afetadas).
     */
    @Test
    void tokenConsumidoPorRequisicaoConcorrente_naoAlteraSenhaELancaExcecao() {
        PasswordResetToken token = tokenValido();

        when(passwordResetTokenRepository.findByToken("token-valido-123")).thenReturn(Optional.of(token));
        when(passwordResetTokenRepository.marcarComoUsadoSeValido(eq("token-valido-123"), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> passwordResetService.changePassord("token-valido-123", "novaSenha123", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token inválido ou expirado.");

        verify(usuarioRepository, never()).findByIdIgnorandoTenant(anyLong());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void tokenExpirado_naoAlteraSenhaELancaExcecao() {
        PasswordResetToken token = tokenValido();
        token.setExpiraEm(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(token));
        // Expirado: a cláusula "t.expiraEm > :agora" do UPDATE atômico não bate com nenhuma linha.
        when(passwordResetTokenRepository.marcarComoUsadoSeValido(eq("token-expirado"), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> passwordResetService.changePassord("token-expirado", "novaSenha123", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token inválido ou expirado.");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void tokenJaUtilizado_naoAlteraSenhaELancaExcecao() {
        PasswordResetToken token = tokenValido();
        token.setUsado(true);

        when(passwordResetTokenRepository.findByToken("token-usado")).thenReturn(Optional.of(token));
        // Já usado: a cláusula "t.usado = false" do UPDATE atômico não bate com nenhuma linha.
        when(passwordResetTokenRepository.marcarComoUsadoSeValido(eq("token-usado"), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> passwordResetService.changePassord("token-usado", "novaSenha123", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token inválido ou expirado.");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void tokenInexistente_lancaExcecao() {
        when(passwordResetTokenRepository.findByToken("token-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.changePassord("token-invalido", "novaSenha123", "novaSenha123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token inválido ou expirado.");
    }

    @Test
    void senhasDiferentes_lancaExcecaoAntesDeConsultarToken() {
        assertThatThrownBy(() -> passwordResetService.changePassord("qualquer-token", "senha1", "senha2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("As senhas nao coincidem");

        verify(passwordResetTokenRepository, never()).findByToken(any());
    }
}

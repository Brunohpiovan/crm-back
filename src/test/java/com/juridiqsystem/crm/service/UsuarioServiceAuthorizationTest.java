package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.UsuarioResponseDto;
import com.juridiqsystem.crm.model.dtos.UsuarioResponseNoAuthDto;
import com.juridiqsystem.crm.model.enums.UserRole;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.exceptions.AccessDeniedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Cobre os 3 níveis de autorização de consulta de usuário (próprio usuário, administrador e
 * usuário sem permissão) tanto em findByIdParaEdicao (GET /usuario/{id}/edicao) quanto em
 * findById (GET /usuario/{id}) — antes da correção, findById não tinha o bypass de administrador,
 * então nem admin conseguia consultar o cadastro de outro usuário por ali.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceAuthorizationTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuarioAlvo() {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        usuario.setPublicId("42");
        usuario.setLogin("alvo@teste.com");
        usuario.setNome("Usuário Alvo");
        usuario.setUrlPicture("assets/img/avatar/padrao.jpeg");
        usuario.setCargo(UserRole.VENDEDOR);
        usuario.setCep("01000-000");
        return usuario;
    }

    private void autenticarComo(String login, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(login, null, grantedAuthorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void proprioUsuario_podeConsultarSeuProprioCadastro() {
        Usuario alvo = usuarioAlvo();
        when(usuarioRepository.findByPublicId("42")).thenReturn(Optional.of(alvo));
        autenticarComo("alvo@teste.com", "ROLE_VENDEDOR");

        UsuarioResponseNoAuthDto resposta = usuarioService.findByIdParaEdicao("42");

        assertThat(resposta.getId()).isEqualTo("42");
        assertThat(resposta.getLogin()).isEqualTo("alvo@teste.com");
    }

    @Test
    void administrador_podeConsultarCadastroDeOutroUsuario() {
        Usuario alvo = usuarioAlvo();
        when(usuarioRepository.findByPublicId("42")).thenReturn(Optional.of(alvo));
        autenticarComo("admin@teste.com", "ROLE_ADMIN", "ROLE_VENDEDOR");

        UsuarioResponseNoAuthDto resposta = usuarioService.findByIdParaEdicao("42");

        assertThat(resposta.getId()).isEqualTo("42");
    }

    @Test
    void usuarioSemPermissao_naoPodeConsultarCadastroDeOutroUsuario() {
        Usuario alvo = usuarioAlvo();
        when(usuarioRepository.findByPublicId("42")).thenReturn(Optional.of(alvo));
        autenticarComo("outro-vendedor@teste.com", "ROLE_VENDEDOR");

        assertThatThrownBy(() -> usuarioService.findByIdParaEdicao("42"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permissão");
    }

    @Test
    void findById_proprioUsuario_podeConsultarSeuProprioCadastro() {
        Usuario alvo = usuarioAlvo();
        when(usuarioRepository.findByPublicId("42")).thenReturn(Optional.of(alvo));
        autenticarComo("alvo@teste.com", "ROLE_VENDEDOR");

        UsuarioResponseDto resposta = usuarioService.findById("42");

        assertThat(resposta.getId()).isEqualTo("42");
    }

    @Test
    void findById_administrador_podeConsultarCadastroDeOutroUsuario() {
        Usuario alvo = usuarioAlvo();
        when(usuarioRepository.findByPublicId("42")).thenReturn(Optional.of(alvo));
        autenticarComo("admin@teste.com", "ROLE_ADMIN", "ROLE_VENDEDOR");

        UsuarioResponseDto resposta = usuarioService.findById("42");

        assertThat(resposta.getId()).isEqualTo("42");
    }

    @Test
    void findById_usuarioSemPermissao_naoPodeConsultarCadastroDeOutroUsuario() {
        Usuario alvo = usuarioAlvo();
        when(usuarioRepository.findByPublicId("42")).thenReturn(Optional.of(alvo));
        autenticarComo("outro-vendedor@teste.com", "ROLE_VENDEDOR");

        assertThatThrownBy(() -> usuarioService.findById("42"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permissão");
    }
}

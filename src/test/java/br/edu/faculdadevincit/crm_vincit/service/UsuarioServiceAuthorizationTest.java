package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioResponseDto;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioResponseNoAuthDto;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.AccessDeniedException;
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
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(alvo));
        autenticarComo("alvo@teste.com", "ROLE_VENDEDOR");

        UsuarioResponseNoAuthDto resposta = usuarioService.findByIdParaEdicao(42L);

        assertThat(resposta.getId()).isEqualTo(42L);
        assertThat(resposta.getLogin()).isEqualTo("alvo@teste.com");
    }

    @Test
    void administrador_podeConsultarCadastroDeOutroUsuario() {
        Usuario alvo = usuarioAlvo();
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(alvo));
        autenticarComo("admin@teste.com", "ROLE_ADMIN", "ROLE_VENDEDOR");

        UsuarioResponseNoAuthDto resposta = usuarioService.findByIdParaEdicao(42L);

        assertThat(resposta.getId()).isEqualTo(42L);
    }

    @Test
    void usuarioSemPermissao_naoPodeConsultarCadastroDeOutroUsuario() {
        Usuario alvo = usuarioAlvo();
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(alvo));
        autenticarComo("outro-vendedor@teste.com", "ROLE_VENDEDOR");

        assertThatThrownBy(() -> usuarioService.findByIdParaEdicao(42L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permissão");
    }

    @Test
    void findById_proprioUsuario_podeConsultarSeuProprioCadastro() {
        Usuario alvo = usuarioAlvo();
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(alvo));
        autenticarComo("alvo@teste.com", "ROLE_VENDEDOR");

        UsuarioResponseDto resposta = usuarioService.findById(42L);

        assertThat(resposta.getId()).isEqualTo(42L);
    }

    @Test
    void findById_administrador_podeConsultarCadastroDeOutroUsuario() {
        Usuario alvo = usuarioAlvo();
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(alvo));
        autenticarComo("admin@teste.com", "ROLE_ADMIN", "ROLE_VENDEDOR");

        UsuarioResponseDto resposta = usuarioService.findById(42L);

        assertThat(resposta.getId()).isEqualTo(42L);
    }

    @Test
    void findById_usuarioSemPermissao_naoPodeConsultarCadastroDeOutroUsuario() {
        Usuario alvo = usuarioAlvo();
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(alvo));
        autenticarComo("outro-vendedor@teste.com", "ROLE_VENDEDOR");

        assertThatThrownBy(() -> usuarioService.findById(42L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permissão");
    }
}

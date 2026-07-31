package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
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
 * UsuarioService.findByIdParaEdicao é o único ponto do sistema que implementa os 3 níveis de
 * autorização de consulta de usuário pedidos pela etapa: próprio usuário, administrador e usuário
 * sem permissão. (UsuarioService.findById, usado por GET /usuario/{id}, só libera o próprio login,
 * mesmo para administradores — não é o caso testado aqui.)
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
}

package com.juridiqsystem.crm.infra.security;

import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.testsupport.TestCargoFactory;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.auth.ClientInfoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre a checagem de usuário bloqueado a cada requisição autenticada — antes desta correção, o
 * SecurityFilter só conferia sessionVersion; um usuário bloqueado pelo administrador DEPOIS de já
 * ter um JWT emitido continuava autenticando normalmente com esse token até a expiração natural
 * (12h). Também cobre o caminho feliz (usuário ativo, sessionVersion batendo) para garantir que a
 * correção não quebrou o fluxo normal.
 */
@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    private static final String SEGREDO_DE_TESTE = "segredo-de-teste-nao-usado-em-producao";
    private static final String PUBLIC_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SecurityLogger securityLogger;

    @Mock
    private ClientInfoService clientInfoService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private FilterChain filterChain;

    private SecurityFilter securityFilter;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", SEGREDO_DE_TESTE);

        securityFilter = new SecurityFilter();
        ReflectionTestUtils.setField(securityFilter, "tokenService", tokenService);
        ReflectionTestUtils.setField(securityFilter, "repository", usuarioRepository);
        ReflectionTestUtils.setField(securityFilter, "securityLogger", securityLogger);
        ReflectionTestUtils.setField(securityFilter, "clientInfoService", clientInfoService);

        lenient().when(request.getRequestURI()).thenReturn("/oportunidade");
        lenient().when(clientInfoService.getClientIp(request)).thenReturn("203.0.113.9");
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private Usuario usuarioBase(boolean bloqueado, int sessaoVersao) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setPublicId(PUBLIC_ID);
        usuario.setEmpresaId(9L);
        usuario.setLogin("usuario@teste.com");
        usuario.setNome("Usuário Teste");
        usuario.setCargo(TestCargoFactory.comum());
        usuario.setBloqueado(bloqueado);
        usuario.setSessaoVersao(sessaoVersao);
        return usuario;
    }

    @Test
    void usuarioAtivo_comSessaoValida_autentica() throws Exception {
        Usuario usuario = usuarioBase(false, 0);
        String token = tokenService.generateToken(usuario);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(usuarioRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(usuario));
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(usuario);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void usuarioBloqueadoDepoisDeEmitidoOToken_naoAutenticaMesmoComTokenValidoENaoExpirado() throws Exception {
        // Token emitido quando o usuário ainda estava ativo (sessionVersion=0)...
        Usuario usuarioNoMomentoDoLogin = usuarioBase(false, 0);
        String token = tokenService.generateToken(usuarioNoMomentoDoLogin);

        // ...mas agora, no momento da requisição, o admin já bloqueou a conta. sessionVersion não
        // mudou (bloquear não é o mesmo fluxo que trocar senha/logout), então sem a checagem de
        // bloqueado este token continuaria autenticando normalmente.
        Usuario usuarioAgora = usuarioBase(true, 0);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(usuarioRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(usuarioAgora));
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void sessaoVersaoDiferente_naoAutentica() throws Exception {
        Usuario usuarioNoMomentoDoLogin = usuarioBase(false, 0);
        String token = tokenService.generateToken(usuarioNoMomentoDoLogin);

        Usuario usuarioAgora = usuarioBase(false, 1); // trocou a senha ou fez logout depois
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(usuarioRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(usuarioAgora));

        securityFilter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void semTokenNoHeader_seguePraFrenteSemAutenticarENuncaConsultaUsuario() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(usuarioRepository, never()).findByPublicId(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}

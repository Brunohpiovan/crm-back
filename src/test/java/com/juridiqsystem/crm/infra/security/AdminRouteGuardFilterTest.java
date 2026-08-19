package com.juridiqsystem.crm.infra.security;

import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.service.auth.ClientInfoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre a camada extra de proteção de /master/** (ADMIN_ROUTE_SECRET + allowlist de IP) — nenhuma
 * das duas substitui a checagem de ROLE_MASTER (feita depois, no SecurityConfiguration); este
 * filtro só decide se a requisição chega a esse ponto. Toda rejeição deve responder 404 (rota
 * "não existe" para quem não tem o segredo/IP certo), nunca 401/403.
 */
@ExtendWith(MockitoExtension.class)
class AdminRouteGuardFilterTest {

    @Mock
    private ClientInfoService clientInfoService;

    @Mock
    private SecurityLogger securityLogger;

    @Mock
    private HttpServletRequest request;

    @Mock
    private FilterChain filterChain;

    private AdminRouteGuardFilter filter;
    private AdminAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new AdminAccessPolicy();
        ReflectionTestUtils.setField(policy, "clientInfoService", clientInfoService);

        filter = new AdminRouteGuardFilter();
        ReflectionTestUtils.setField(filter, "adminAccessPolicy", policy);
        ReflectionTestUtils.setField(filter, "clientInfoService", clientInfoService);
        ReflectionTestUtils.setField(filter, "securityLogger", securityLogger);
        lenient().when(request.getContextPath()).thenReturn("");
    }

    private void configurar(String secret, String ipAllowlist) {
        ReflectionTestUtils.setField(policy, "configuredSecret", secret);
        ReflectionTestUtils.setField(policy, "ipAllowlistRaw", ipAllowlist);
        ReflectionTestUtils.invokeMethod(policy, "init");
    }

    @Test
    void rotaNaoAdministrativa_passaDiretoIndependenteDaConfiguracao() throws Exception {
        configurar("segredo-123", "");
        when(request.getRequestURI()).thenReturn("/usuario");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void semSegredoNemAllowlistConfigurados_deixaPassar() throws Exception {
        configurar("", "");
        when(request.getRequestURI()).thenReturn("/master/empresas");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void segredoAusente_bloqueiaCom404GenericoSemRevelarMotivo() throws Exception {
        configurar("segredo-correto", "");
        when(request.getRequestURI()).thenReturn("/master/empresas");
        when(request.getHeader("X-Admin-Route-Secret")).thenReturn(null);
        when(clientInfoService.getClientIp(request)).thenReturn("203.0.113.9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
        assertThat(response.getContentAsString()).doesNotContain("segredo", "IP", "Admin");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void segredoIncorreto_bloqueiaCom404() throws Exception {
        configurar("segredo-correto", "");
        when(request.getRequestURI()).thenReturn("/master/empresas");
        when(request.getHeader("X-Admin-Route-Secret")).thenReturn("segredo-errado");
        when(clientInfoService.getClientIp(request)).thenReturn("203.0.113.9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void segredoCorreto_semAllowlist_deixaPassar() throws Exception {
        configurar("segredo-correto", "");
        when(request.getRequestURI()).thenReturn("/master/empresas");
        when(request.getHeader("X-Admin-Route-Secret")).thenReturn("segredo-correto");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void ipForaDaAllowlist_bloqueiaCom404MesmoComSegredoCorreto() throws Exception {
        configurar("segredo-correto", "10.0.0.0/8");
        when(request.getRequestURI()).thenReturn("/master/empresas");
        when(request.getHeader("X-Admin-Route-Secret")).thenReturn("segredo-correto");
        when(clientInfoService.getClientIp(request)).thenReturn("203.0.113.9");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(any(), any());
    }

    /**
     * A allowlist de IP também é aplicada no login (AuthenticationService), e lá o segredo de rota
     * não vale — o frontend público não teria onde guardá-lo. isIpAllowed precisa decidir sozinho.
     */
    @Test
    void isIpAllowed_ignoraSegredoDeRotaEOlhaSoOIp() {
        configurar("segredo-correto", "10.0.0.0/8");
        when(clientInfoService.getClientIp(request)).thenReturn("10.1.2.3");

        assertThat(policy.isIpAllowed(request)).isTrue();

        when(clientInfoService.getClientIp(request)).thenReturn("203.0.113.9");
        assertThat(policy.isIpAllowed(request)).isFalse();
    }

    @Test
    void isIpAllowed_semAllowlistConfigurada_liberaQualquerIp() {
        configurar("", "");

        assertThat(policy.isIpAllowed(request)).isTrue();
    }

    @Test
    void ipNaAllowlistESegredoCorreto_deixaPassar() throws Exception {
        configurar("segredo-correto", "10.0.0.0/8");
        when(request.getRequestURI()).thenReturn("/master/empresas");
        when(request.getHeader("X-Admin-Route-Secret")).thenReturn("segredo-correto");
        when(clientInfoService.getClientIp(request)).thenReturn("10.1.2.3");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}

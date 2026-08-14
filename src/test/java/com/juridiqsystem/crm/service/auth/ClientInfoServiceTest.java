package com.juridiqsystem.crm.service.auth;

import com.juridiqsystem.crm.infra.security.TrustedProxyResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Cobre a correção de spoofing de IP: um atacante que manda X-Forwarded-For diretamente pro
 * backend (sem passar por um proxy confiável de verdade) não deve conseguir controlar o IP que o
 * sistema usa pra rate limiting/auditoria — ver TrustedProxyResolver.
 */
@ExtendWith(MockitoExtension.class)
class ClientInfoServiceTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private GeoLocationService geoLocationService;

    @Mock
    private TrustedProxyResolver trustedProxyResolver;

    @InjectMocks
    private ClientInfoService clientInfoService;

    @Test
    void semProxyConfiavel_ignoraXForwardedForForjadoEUsaPeerDireto() {
        when(request.getRemoteAddr()).thenReturn("203.0.113.7");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
        when(trustedProxyResolver.isTrusted("203.0.113.7")).thenReturn(false);

        String ip = clientInfoService.getClientIp(request);

        assertThat(ip).isEqualTo("203.0.113.7");
    }

    @Test
    void comProxyConfiavel_usaOPrimeiroIpDoXForwardedFor() {
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.23, 10.0.0.1");
        when(trustedProxyResolver.isTrusted("10.0.0.1")).thenReturn(true);

        String ip = clientInfoService.getClientIp(request);

        assertThat(ip).isEqualTo("198.51.100.23");
    }

    @Test
    void headerComValorNaoParecendoIp_eIgnoradoMesmoComProxyConfiavel() {
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("<script>alert(1)</script>");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(trustedProxyResolver.isTrusted("10.0.0.1")).thenReturn(true);

        String ip = clientInfoService.getClientIp(request);

        assertThat(ip).isEqualTo("10.0.0.1");
    }
}

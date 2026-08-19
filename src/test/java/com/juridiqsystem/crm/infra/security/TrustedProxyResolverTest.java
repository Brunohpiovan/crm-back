package com.juridiqsystem.crm.infra.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre a correção central de "nunca confiar cegamente em X-Forwarded-For": sem TRUSTED_PROXIES
 * configurado (comportamento padrão), nenhum IP é considerado proxy confiável — é esse resultado
 * que faz ClientInfoService.getClientIp ignorar completamente os headers e usar sempre o peer TCP
 * direto, fechando o bypass de rate limiting (login, recuperação de senha) via header forjado.
 */
class TrustedProxyResolverTest {

    private TrustedProxyResolver build(String trustedProxiesCsv) {
        TrustedProxyResolver resolver = new TrustedProxyResolver();
        ReflectionTestUtils.setField(resolver, "trustedProxiesRaw", trustedProxiesCsv);
        ReflectionTestUtils.invokeMethod(resolver, "init");
        return resolver;
    }

    @Test
    void semConfiguracao_nenhumIpEhConfiavel() {
        TrustedProxyResolver resolver = build("");

        assertThat(resolver.hasTrustedProxies()).isFalse();
        assertThat(resolver.isTrusted("203.0.113.7")).isFalse();
        assertThat(resolver.isTrusted("127.0.0.1")).isFalse();
    }

    @Test
    void comFaixaConfigurada_apenasIpsDaFaixaSaoConfiaveis() {
        TrustedProxyResolver resolver = build("10.0.0.0/8");

        assertThat(resolver.hasTrustedProxies()).isTrue();
        assertThat(resolver.isTrusted("10.5.5.5")).isTrue();
        assertThat(resolver.isTrusted("203.0.113.7")).isFalse();
    }

    @Test
    void ipNulo_nuncaEhConfiavel() {
        TrustedProxyResolver resolver = build("10.0.0.0/8");

        assertThat(resolver.isTrusted(null)).isFalse();
    }
}

package com.juridiqsystem.crm.infra.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Decide se o peer TCP direto da requisição (request.getRemoteAddr(), nunca controlável pelo
 * cliente) é um proxy/load balancer confiável — único caso em que faz sentido olhar para
 * X-Forwarded-For/X-Real-IP, headers que QUALQUER cliente pode forjar livremente. Sem essa
 * checagem, ClientInfoService.getClientIp confiaria cegamente no que o atacante mandasse,
 * quebrando rate limiting por IP (login, recuperação de senha, WhatsApp) e qualquer allowlist de
 * IP administrativa.
 *
 * Configurado via TRUSTED_PROXIES (CSV de IPs/CIDRs, ex.: "10.0.0.0/8,172.16.0.5"). Vazio por
 * padrão — nesse caso NENHUM header é confiado (fail-safe: usa sempre o peer TCP direto), até que
 * alguém configure explicitamente a faixa real do reverse proxy/load balancer de produção.
 */
@Slf4j
@Component
public class TrustedProxyResolver {

    @Value("${app.trusted-proxies:}")
    private String trustedProxiesRaw;

    private CidrMatcherList matchers = CidrMatcherList.parse("", "TRUSTED_PROXIES");

    @PostConstruct
    void init() {
        matchers = CidrMatcherList.parse(trustedProxiesRaw, "TRUSTED_PROXIES");
        if (matchers.isEmpty()) {
            log.info("TRUSTED_PROXIES não configurado — X-Forwarded-For/X-Real-IP serão ignorados; getClientIp() sempre usa o peer TCP direto.");
        }
    }

    public boolean isTrusted(String remoteAddr) {
        return matchers.matches(remoteAddr);
    }

    public boolean hasTrustedProxies() {
        return !matchers.isEmpty();
    }
}

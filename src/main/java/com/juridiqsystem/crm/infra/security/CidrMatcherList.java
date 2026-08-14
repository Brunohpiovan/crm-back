package com.juridiqsystem.crm.infra.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista de IPs/CIDRs (IPv4 e IPv6) construída a partir de um CSV vindo de configuração, usada por
 * TrustedProxyResolver e pela allowlist de IP administrativa (AdminRouteGuardFilter). Entradas
 * inválidas são logadas e ignoradas, nunca derrubam a aplicação — um typo na env var não pode virar
 * uma negação de serviço pra todo mundo (nem, no caso da allowlist, abrir a rota pra qualquer IP).
 */
@Slf4j
public final class CidrMatcherList {

    private final List<IpAddressMatcher> matchers;

    private CidrMatcherList(List<IpAddressMatcher> matchers) {
        this.matchers = matchers;
    }

    public static CidrMatcherList parse(String csv, String configName) {
        List<IpAddressMatcher> parsed = new ArrayList<>();
        if (csv != null) {
            for (String entry : csv.split(",")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    parsed.add(new IpAddressMatcher(trimmed));
                } catch (Exception e) {
                    log.warn("{}: entrada ignorada por ser um IP/CIDR inválido: {}", configName, trimmed);
                }
            }
        }
        return new CidrMatcherList(List.copyOf(parsed));
    }

    public boolean matches(String ip) {
        if (ip == null || matchers.isEmpty()) {
            return false;
        }
        for (IpAddressMatcher matcher : matchers) {
            try {
                if (matcher.matches(ip)) {
                    return true;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return matchers.isEmpty();
    }
}

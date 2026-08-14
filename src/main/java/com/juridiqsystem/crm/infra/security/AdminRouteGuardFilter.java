package com.juridiqsystem.crm.infra.security;

import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.service.auth.ClientInfoService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Camada ADICIONAL de proteção para /master/** (área do usuário master, super-admin
 * multi-empresa) — roda ANTES do SecurityFilter (nem chega a decodificar o JWT se bloquear aqui).
 * NÃO substitui autenticação/autorização: mesmo passando por este filtro, a requisição ainda
 * precisa de um JWT válido com ROLE_MASTER (ver SecurityConfiguration) para ser atendida. É só
 * obscuridade + restrição de origem, defesa em profundidade caso um JWT de MASTER vaze.
 *
 * Duas checagens independentes, cada uma só ativa se a env var correspondente estiver configurada
 * (vazio = checagem desligada, comportamento atual preservado até alguém configurar):
 *  - ADMIN_ROUTE_SECRET: header X-Admin-Route-Secret precisa bater exatamente (comparação em
 *    tempo constante, para não vazar o segredo por timing).
 *  - ADMIN_IP_ALLOWLIST: IP do cliente (ver ClientInfoService — já protegido contra spoofing de
 *    X-Forwarded-For) precisa estar numa faixa liberada.
 *
 * Em qualquer falha, responde 404 (não 401/403): o objetivo é que a rota pareça não existir para
 * quem não tem o segredo/IP certo, não confirmar que ela existe e negar acesso. O motivo da
 * rejeição nunca é exposto na resposta nem em log público — só no SecurityLogger interno.
 */
@Component
public class AdminRouteGuardFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH_PREFIX = "/master";
    private static final String SECRET_HEADER = "X-Admin-Route-Secret";

    @Value("${app.admin.route-secret:}")
    private String configuredSecret;

    @Value("${app.admin.ip-allowlist:}")
    private String ipAllowlistRaw;

    @Autowired
    private ClientInfoService clientInfoService;

    @Autowired
    private SecurityLogger securityLogger;

    private CidrMatcherList ipAllowlist = CidrMatcherList.parse("", "ADMIN_IP_ALLOWLIST");

    @PostConstruct
    void init() {
        ipAllowlist = CidrMatcherList.parse(ipAllowlistRaw, "ADMIN_IP_ALLOWLIST");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isAdminPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isSecretConfigured() && !secretMatches(request)) {
            deny(request, response, "Segredo de rota administrativa ausente ou inválido");
            return;
        }

        if (!ipAllowlist.isEmpty() && !ipAllowlist.matches(clientInfoService.getClientIp(request))) {
            deny(request, response, "IP fora da allowlist administrativa");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAdminPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
        return path.equals(ADMIN_PATH_PREFIX) || path.startsWith(ADMIN_PATH_PREFIX + "/");
    }

    private boolean isSecretConfigured() {
        return configuredSecret != null && !configuredSecret.isBlank();
    }

    private boolean secretMatches(HttpServletRequest request) {
        String provided = request.getHeader(SECRET_HEADER);
        if (provided == null || provided.isEmpty()) {
            return false;
        }
        byte[] a = provided.getBytes(StandardCharsets.UTF_8);
        byte[] b = configuredSecret.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    private void deny(HttpServletRequest request, HttpServletResponse response, String motivo) throws IOException {
        securityLogger.log(SecurityEventType.ADMIN_ROUTE_DENIED, motivo, null,
                clientInfoService.getClientIp(request), request.getRequestURI());
        // 404 (não 401/403) de propósito: a rota deve parecer inexistente para quem não tem o
        // segredo/IP certo. O corpo nunca menciona o motivo real.
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Recurso não encontrado.\"}");
    }
}

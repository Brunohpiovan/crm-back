package com.juridiqsystem.crm.infra.security;

import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.service.auth.ClientInfoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Camada ADICIONAL de proteção para /master/** (área do usuário master, super-admin
 * multi-empresa) — roda ANTES do SecurityFilter (nem chega a decodificar o JWT se bloquear aqui).
 * NÃO substitui autenticação/autorização: mesmo passando por este filtro, a requisição ainda
 * precisa de um JWT válido com ROLE_MASTER (ver SecurityConfiguration) para ser atendida. É só
 * obscuridade + restrição de origem, defesa em profundidade caso um JWT de MASTER vaze.
 *
 * As regras em si vivem em AdminAccessPolicy, porque o login também precisa aplicá-las (ver
 * AuthenticationService): um MASTER vindo de uma origem não autorizada nem chega a receber token.
 *
 * Em qualquer falha, responde 404 (não 401/403): o objetivo é que a rota pareça não existir para
 * quem não tem o segredo/IP certo, não confirmar que ela existe e negar acesso. O motivo da
 * rejeição nunca é exposto na resposta nem em log público — só no SecurityLogger interno.
 */
@Component
public class AdminRouteGuardFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH_PREFIX = "/master";

    @Autowired
    private AdminAccessPolicy adminAccessPolicy;

    @Autowired
    private ClientInfoService clientInfoService;

    @Autowired
    private SecurityLogger securityLogger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isAdminPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String motivo = adminAccessPolicy.denialReason(request);
        if (motivo != null) {
            deny(request, response, motivo);
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

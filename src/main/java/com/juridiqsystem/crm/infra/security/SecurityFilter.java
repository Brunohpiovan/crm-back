package com.juridiqsystem.crm.infra.security;

import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.auth.ClientInfoService;
import com.juridiqsystem.crm.service.exceptions.UserNotFoundException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private SecurityLogger securityLogger;

    @Autowired
    private ClientInfoService clientInfoService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoveryToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        DecodedJWT decoded = tokenService.validateToken(token);
        if (decoded == null) {
            // Assinatura inválida ou token expirado (TokenService não distingue os dois casos
            // hoje) — segue sem popular o SecurityContext, deixando o filtro de autorização
            // rejeitar como 401 mais adiante se a rota exigir autenticação.
            securityLogger.log(SecurityEventType.INVALID_TOKEN, null, null,
                    clientInfoService.getClientIp(request), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // empresaId precisa ir pro TenantContext ANTES de buscar o Usuario: Usuario é
        // @TenantId, então sem o tenant já resolvido essa busca por id não encontraria nada
        // (ver TenantIdentifierResolver). Efeito colateral bom: se o claim empresaId não bater
        // com o empresa_id real do usuário (token adulterado/de um usuário movido de empresa),
        // o findById simplesmente não encontra nada, e a autenticação falha com segurança.
        Long empresaId = decoded.getClaim("empresaId").asLong();
        TenantContext.set(empresaId);
        try {
            String userPublicId = decoded.getSubject();
            Usuario user = repository.findByPublicId(userPublicId)
                    .orElseThrow(() -> new UserNotFoundException("Usuário do token nao encontrado."));

            // Token emitido antes de um logout/troca de senha desse usuário: a claim
            // sessionVersion (gravada no momento do login) não bate mais com o valor atual em
            // banco (incrementado no logout/troca de senha). Sem essa checagem, o JWT continuaria
            // válido até a expiração natural (12h) mesmo depois de "sair" da conta. Claim ausente
            // (token emitido antes desta migration) é tratado como inválido — força novo login.
            Integer tokenSessionVersion = decoded.getClaim("sessionVersion").asInt();
            if (tokenSessionVersion == null || !tokenSessionVersion.equals(user.getSessaoVersao())) {
                securityLogger.log(SecurityEventType.EXPIRED_TOKEN, "Sessão invalidada (logout ou troca de senha)",
                        user.getLogin(), clientInfoService.getClientIp(request), request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String recoveryToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if(authHeader == null)return null;
        return authHeader.replace("Bearer ","");
    }
}

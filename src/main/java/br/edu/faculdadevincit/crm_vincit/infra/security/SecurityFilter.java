package br.edu.faculdadevincit.crm_vincit.infra.security;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.UserNotFoundException;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoveryToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        DecodedJWT decoded = tokenService.validateToken(token);
        if (decoded == null) {
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

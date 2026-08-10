package com.juridiqsystem.crm.service.auth;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.infra.security.TokenService;
import com.juridiqsystem.crm.model.Acesso;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.AuthenticationDTO;
import com.juridiqsystem.crm.model.dtos.LoginResponseDTO;
import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.service.AcessoService;
import com.juridiqsystem.crm.service.exceptions.TooManyRequestsException;
import com.juridiqsystem.crm.service.exceptions.UsuarioBloqueadoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    @Autowired
    private AuthenticationManager authenticationManager;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private HttpServletRequest request;
    
    @Autowired
    private AcessoService acessoService;


    @Autowired
    private ClientInfoService clientInfoService;

    @Autowired
    private LoginRateLimiter loginRateLimiter;

    @Autowired
    private SecurityLogger securityLogger;

    public LoginResponseDTO login(AuthenticationDTO data) {
        String clientIp = clientInfoService.getClientIp(request);
        String rateLimitKey = clientIp + "|" + data.codigoEmpresa() + "|" + data.login().trim().toLowerCase();
        if (loginRateLimiter.isBlocked(rateLimitKey)) {
            securityLogger.log(SecurityEventType.RATE_LIMIT_TRIGGERED, "Login bloqueado por excesso de tentativas",
                    data.codigoEmpresa() + "|" + data.login(), clientIp, "/auth/login");
            throw new TooManyRequestsException("Muitas tentativas de login. Tente novamente em instantes.");
        }

        try {
            // login é único por Empresa, não globalmente — o "username" que o Spring Security
            // enxerga precisa carregar os dois pra desambiguar (ver AuthorizationService).
            String username = data.codigoEmpresa() + "|" + data.login();
            var usernamePassword = new UsernamePasswordAuthenticationToken(username, data.senha());
            Authentication auth;
            try {
                auth = authenticationManager.authenticate(usernamePassword);
            } catch (AuthenticationException ex) {
                loginRateLimiter.registerFailure(rateLimitKey);
                securityLogger.log(SecurityEventType.LOGIN_FAILED, "Credenciais inválidas", username, clientIp, "/auth/login");
                throw ex;
            }
            var usuario = (Usuario) auth.getPrincipal();
            var token = tokenService.generateToken(usuario);
            loginRateLimiter.reset(rateLimitKey);

            if(usuario.getBloqueado()){
                securityLogger.log(SecurityEventType.ACCOUNT_LOCKED, "Login recusado: usuário bloqueado pelo administrador",
                        username, clientIp, "/auth/login");
                throw new UsuarioBloqueadoException("Você não tem permissão para acessar o sistema.");
            }

            securityLogger.log(SecurityEventType.LOGIN_SUCCESS, null, username, clientIp, "/auth/login");

            // O login em si não passa pelo SecurityFilter (é permitAll, sem Authorization
            // header), então o TenantContext ainda não foi populado neste ponto — sem isso,
            // o insert de Acesso (que é @TenantId) cairia no sentinel -1 e violaria a FK
            // pra empresa. Aqui já temos o Usuario autenticado, então setamos manualmente.
            TenantContext.set(usuario.getEmpresaId());
            try {
                Acesso acesso = clientInfoService.createLogAcesso(usuario, request);
                String logId = acessoService.save(acesso);
                return new LoginResponseDTO(token, logId);
            } finally {
                TenantContext.clear();
            }
        } catch (InternalAuthenticationServiceException ex) {
            throw new InternalAuthenticationServiceException("Erro na autenticação: " + ex.getMessage(), ex);
        }
    }


}

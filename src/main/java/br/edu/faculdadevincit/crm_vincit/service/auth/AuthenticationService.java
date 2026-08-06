package br.edu.faculdadevincit.crm_vincit.service.auth;

import br.edu.faculdadevincit.crm_vincit.infra.security.TenantContext;
import br.edu.faculdadevincit.crm_vincit.infra.security.TokenService;
import br.edu.faculdadevincit.crm_vincit.model.Acesso;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.AuthenticationDTO;
import br.edu.faculdadevincit.crm_vincit.model.dtos.LoginResponseDTO;
import br.edu.faculdadevincit.crm_vincit.service.AcessoService;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.UsuarioBloqueadoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    public LoginResponseDTO login(AuthenticationDTO data) {
        try {
            // login é único por Empresa, não globalmente — o "username" que o Spring Security
            // enxerga precisa carregar os dois pra desambiguar (ver AuthorizationService).
            String username = data.codigoEmpresa() + "|" + data.login();
            var usernamePassword = new UsernamePasswordAuthenticationToken(username, data.senha());
            var auth = authenticationManager.authenticate(usernamePassword);
            var usuario = (Usuario) auth.getPrincipal();
            var token = tokenService.generateToken(usuario);

            if(usuario.getBloqueado()){
                throw new UsuarioBloqueadoException("Você não tem permissão para acessar o sistema.");
            }

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

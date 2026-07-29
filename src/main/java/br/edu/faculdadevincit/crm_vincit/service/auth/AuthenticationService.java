package br.edu.faculdadevincit.crm_vincit.service.auth;

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
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.senha());
            var auth = authenticationManager.authenticate(usernamePassword);
            var usuario = (Usuario) auth.getPrincipal();
            var token = tokenService.generateToken(usuario);

            if(usuario.getBloqueado()){
                throw new UsuarioBloqueadoException("Você não tem permissão para acessar o sistema.");
            }
            Acesso acesso = clientInfoService.createLogAcesso(usuario, request);
            Long logId = acessoService.save(acesso);
            return new LoginResponseDTO(token,logId);
        } catch (InternalAuthenticationServiceException ex) {
            throw new InternalAuthenticationServiceException("Erro na autenticação");
        }
    }


}

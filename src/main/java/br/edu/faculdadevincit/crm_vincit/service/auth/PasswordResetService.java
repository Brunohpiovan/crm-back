package br.edu.faculdadevincit.crm_vincit.service.auth;

import br.edu.faculdadevincit.crm_vincit.infra.security.TokenService;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ApiResponse;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class PasswordResetService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ApiResponse changePassord(String token, String password,String password2){
        try {
            String email = tokenService.validateToken(token);
            if (Objects.equals(email, "")) {
                throw new IllegalArgumentException("Token inválido ou expirado.");
            }
            if(!Objects.equals(password, password2)){
                throw new RuntimeException("As senhas nao coincidem");
            }
            Usuario user = (Usuario) usuarioRepository.findByLogin(email).orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
            user.setSenha(passwordEncoder.encode(password));
            usuarioRepository.save(user);
            return new ApiResponse("Senha alterada com sucesso.");
        } catch (UserNotFoundException ex) {
            throw new UserNotFoundException("Usuario nao encontrado");
        }catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }
}
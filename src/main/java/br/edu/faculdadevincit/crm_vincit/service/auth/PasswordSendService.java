package br.edu.faculdadevincit.crm_vincit.service.auth;

import br.edu.faculdadevincit.crm_vincit.infra.security.TokenService;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ApiResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EmailDTO;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.InvalidEmailException;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
public class PasswordSendService {
    @Autowired
    private EmailService emailService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenService tokenService;
    public ApiResponse SendEmailRecovery(EmailDTO mail) {
        String email = mail.email();
        try {
            String decodedEmail = URLDecoder.decode(email, StandardCharsets.UTF_8).trim();
            if (decodedEmail.isEmpty()) {
                throw new InvalidEmailException("Endereço de e-mail inválido.");
            }
            Usuario user = (Usuario) usuarioRepository.findByLogin(email).orElseThrow(() ->new UserNotFoundException("Usuário não encontrado."));
            String token = this.tokenService.generateToken(user);
            String recoveryUrl = "http://localhost:4200/change-password?token=" + token;
            String recoveryMessage = "Olá, \n\n"
                    + "Recebemos uma solicitação para recuperar sua senha em nosso sistema. Para continuar, "
                    + "clique no link abaixo e siga as instruções para criar uma nova senha:\n\n"
                    + recoveryUrl + "\n\n"
                    + "Se você não solicitou a recuperação da senha, por favor, ignore este e-mail. "
                    + "Nenhuma alteração será realizada em sua conta.\n\n"
                    + "Atenciosamente,\n"
                    + "Equipe VINCIT\n";
            emailService.sendEmail(decodedEmail, "Recuperação de Senha - crm_vincit", recoveryMessage);
            return new ApiResponse("Instruções de recuperação de senha enviadas para seu e-mail.");
        } catch (UserNotFoundException ex) {
            throw new UserNotFoundException(ex.getMessage());
        }
    }
}
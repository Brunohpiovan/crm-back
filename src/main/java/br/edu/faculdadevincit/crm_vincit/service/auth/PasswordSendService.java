package br.edu.faculdadevincit.crm_vincit.service.auth;

import br.edu.faculdadevincit.crm_vincit.model.PasswordResetToken;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ApiResponse;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EmailDTO;
import br.edu.faculdadevincit.crm_vincit.repository.PasswordResetTokenRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.InvalidEmailException;
import br.edu.faculdadevincit.crm_vincit.service.exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class PasswordSendService {
    private static final int TOKEN_VALIDITY_MINUTES = 30;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public ApiResponse SendEmailRecovery(EmailDTO mail) {
        String email = mail.email();
        try {
            String decodedEmail = URLDecoder.decode(email, StandardCharsets.UTF_8).trim();
            if (decodedEmail.isEmpty()) {
                throw new InvalidEmailException("Endereço de e-mail inválido.");
            }
            Usuario user = (Usuario) usuarioRepository.findByLogin(decodedEmail).orElseThrow(() ->new UserNotFoundException("Usuário não encontrado."));

            String token = generateSecureToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUsuarioId(user.getId());
            resetToken.setCriadoEm(LocalDateTime.now());
            resetToken.setExpiraEm(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES));
            resetToken.setUsado(false);
            passwordResetTokenRepository.save(resetToken);

            String recoveryUrl = frontendUrl + "/change-password?token=" + token;
            String recoveryMessage = "Olá, \n\n"
                    + "Recebemos uma solicitação para recuperar sua senha em nosso sistema. Para continuar, "
                    + "clique no link abaixo e siga as instruções para criar uma nova senha:\n\n"
                    + recoveryUrl + "\n\n"
                    + "Este link é válido por " + TOKEN_VALIDITY_MINUTES + " minutos e pode ser usado apenas uma vez.\n\n"
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

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}

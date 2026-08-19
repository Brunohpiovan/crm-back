package com.juridiqsystem.crm.service.auth;

import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.model.PasswordResetToken;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.ApiResponse;
import com.juridiqsystem.crm.model.dtos.EmailDTO;
import com.juridiqsystem.crm.repository.PasswordResetTokenRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.exceptions.InvalidEmailException;
import com.juridiqsystem.crm.service.exceptions.TooManyRequestsException;
import com.resend.core.exception.ResendException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class PasswordSendService {
    private static final int TOKEN_VALIDITY_MINUTES = 30;

    /**
     * Mesma resposta independentemente de o e-mail existir ou não no sistema: revelar a
     * diferença permitiria enumerar quais e-mails estão cadastrados (respondendo, por exemplo,
     * com um erro só para e-mails inexistentes).
     */
    private static final String GENERIC_RESPONSE_MESSAGE =
            "Se o e-mail informado estiver cadastrado, você receberá instruções de recuperação de senha em instantes.";

    @Autowired
    private EmailService emailService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private ClientInfoService clientInfoService;

    @Autowired
    private PasswordRecoveryRateLimiter rateLimiter;

    @Autowired
    private SecurityLogger securityLogger;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.frontend.reset-password-path}")
    private String resetPasswordPath;

    public ApiResponse SendEmailRecovery(EmailDTO mail, HttpServletRequest request) throws ResendException {
        if (!rateLimiter.tryAcquire(clientInfoService.getClientIp(request))) {
            throw new TooManyRequestsException("Muitas solicitações. Tente novamente mais tarde.");
        }

        String decodedEmail = URLDecoder.decode(mail.email(), StandardCharsets.UTF_8).trim();
        if (decodedEmail.isEmpty()) {
            throw new InvalidEmailException("Endereço de e-mail inválido.");
        }

        // findByEmpresaCodigoAndLogin (nativa) de propósito: este fluxo é anterior a qualquer
        // autenticação (TenantContext vazio), e login não é mais único globalmente — ver
        // comentário do método no UsuarioRepository.
        Optional<Usuario> userOpt = usuarioRepository.findByEmpresaCodigoAndLogin(mail.codigoEmpresa(), decodedEmail);
        if (userOpt.isPresent()) {
            Usuario user = userOpt.get();

            String token = generateSecureToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUsuarioId(user.getId());
            resetToken.setCriadoEm(LocalDateTime.now());
            resetToken.setExpiraEm(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES));
            resetToken.setUsado(false);
            passwordResetTokenRepository.save(resetToken);

            String recoveryUrl = frontendUrl + resetPasswordPath + "?token=" + token;
            String recoveryMessage = "Olá, \n\n"
                    + "Recebemos uma solicitação para recuperar sua senha em nosso sistema. Para continuar, "
                    + "clique no link abaixo e siga as instruções para criar uma nova senha:\n\n"
                    + recoveryUrl + "\n\n"
                    + "Este link é válido por " + TOKEN_VALIDITY_MINUTES + " minutos e pode ser usado apenas uma vez.\n\n"
                    + "Se você não solicitou a recuperação da senha, por favor, ignore este e-mail. "
                    + "Nenhuma alteração será realizada em sua conta.\n\n"
                    + "Atenciosamente,\n"
                    + "Equipe JuridiqSystem\n";
            emailService.sendEmail(decodedEmail, "Recuperação de Senha - JuridiqSystem", recoveryMessage);
        }

        // Loga a tentativa independentemente de o e-mail existir ou não — o canal do Discord é
        // interno (não é a resposta HTTP, que continua genérica), então não há risco de
        // enumeração aqui, só valor de auditoria (padrão de pedidos de reset por IP/empresa).
        securityLogger.log(SecurityEventType.PASSWORD_RESET_REQUEST, "codigoEmpresa=" + mail.codigoEmpresa(), decodedEmail,
                clientInfoService.getClientIp(request), "/recover-password");

        return new ApiResponse(GENERIC_RESPONSE_MESSAGE);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}

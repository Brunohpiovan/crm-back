package com.juridiqsystem.crm.service.auth;

import com.juridiqsystem.crm.model.Email;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.EmailRequestDTO;
import com.juridiqsystem.crm.repository.EmailRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.juridiqsystem.crm.service.exceptions.TooManyRequestsException;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class EmailService {

    // Envio de e-mail avulso custa dinheiro (Resend cobra por envio) e pode ser abusado por uma
    // conta comprometida pra disparar spam em massa — limite por remetente, não por IP (endpoint
    // autenticado, várias pessoas podem legitimamente usar o mesmo IP de escritório).
    private static final int MAX_EMAILS_PER_WINDOW = 20;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    @Autowired
    private Resend resend;

    @Value("${resend.from}")
    private String remetentePadrao;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private SlidingWindowRateLimiter rateLimiter;

    /**
     * Único efeito colateral deste método é o envio em si (sem persistência), então é seguro
     * reexecutar o método inteiro em caso de falha transitória de rede/API do Resend — diferente de
     * {@link #enviarEmail(EmailRequestDTO)}, que também persiste um registro de histórico depois
     * do envio e por isso não é retentado (reenviar arriscaria duplicar o e-mail sem motivo se a
     * falha real estivesse só na gravação do histórico).
     */
    @Retryable(retryFor = ResendException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void sendEmail(String to, String subject, String text) throws ResendException {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(remetentePadrao)
                .to(to.toLowerCase())
                .subject(subject)
                .text(text)
                .build();
        resend.emails().send(params);
    }

    public void enviarEmail(EmailRequestDTO email) throws ResendException {
        if (!rateLimiter.tryAcquire("email-enviar:" + email.getId_remetente(), MAX_EMAILS_PER_WINDOW, RATE_LIMIT_WINDOW)) {
            throw new TooManyRequestsException("Muitos e-mails enviados em um curto intervalo. Tente novamente em instantes.");
        }

        Usuario remetente = usuarioRepository.findByPublicId(email.getId_remetente())
                .orElseThrow(() -> new RuntimeException("Remetente não encontrado"));

        CreateEmailOptions.Builder params = CreateEmailOptions.builder()
                .from(remetentePadrao)
                .to(email.getDestinatario().toLowerCase())
                .subject(email.getAssunto())
                .html(email.getCorpo());

        if (email.getAnexos() != null && !email.getAnexos().isEmpty()) {
            List<Attachment> attachments = new ArrayList<>();
            for (MultipartFile anexo : email.getAnexos()) {
                try {
                    String conteudoBase64 = Base64.getEncoder().encodeToString(anexo.getBytes());
                    attachments.add(Attachment.builder()
                            .fileName(anexo.getOriginalFilename())
                            .content(conteudoBase64)
                            .build());
                } catch (IOException e) {
                    throw new RuntimeException("Erro ao anexar arquivo: " + anexo.getOriginalFilename(), e);
                }
            }
            params.attachments(attachments);
        }

        resend.emails().send(params.build());
        Email newEmail = new Email(email, remetente);
        newEmail.setCriadoEm(LocalDateTime.now());
        emailRepository.save(newEmail);
    }
}

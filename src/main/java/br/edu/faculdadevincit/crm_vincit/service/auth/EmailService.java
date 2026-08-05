package br.edu.faculdadevincit.crm_vincit.service.auth;

import br.edu.faculdadevincit.crm_vincit.model.Email;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.EmailRequestDTO;
import br.edu.faculdadevincit.crm_vincit.repository.EmailRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import io.jsonwebtoken.io.IOException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;


@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailRepository emailRepository;

    /**
     * Único efeito colateral deste método é o envio em si (sem persistência), então é seguro
     * reexecutar o método inteiro em caso de falha transitória de rede/SMTP — diferente de
     * {@link #enviarEmail(EmailRequestDTO)}, que também persiste um registro de histórico depois
     * do envio e por isso não é retentado (reenviar arriscaria duplicar o e-mail sem motivo se a
     * falha real estivesse só na gravação do histórico).
     */
    @Retryable(retryFor = MailException.class, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to.toLowerCase());
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    public void enviarEmail(EmailRequestDTO email) throws MessagingException, UnsupportedEncodingException {
        Usuario remetente = usuarioRepository.findById(email.getId_remetente())
                .orElseThrow(() -> new RuntimeException("Remetente não encontrado"));

        MimeMessage mensagem = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");
        helper.setTo(email.getDestinatario().toLowerCase());
        helper.setSubject(email.getAssunto());
        helper.setText(email.getCorpo(), true);
        helper.setFrom("sistema1@faculdadevincit.edu.br", "Faculdade Vincit");
        if (email.getAnexos() != null && !email.getAnexos().isEmpty()) {
            for (MultipartFile anexo : email.getAnexos()) {
                try {
                    helper.addAttachment(anexo.getOriginalFilename(), anexo);
                } catch (IOException e) {
                    throw new RuntimeException("Erro ao anexar arquivo: " + anexo.getOriginalFilename(), e);
                }
            }
        }
        mailSender.send(mensagem);
        Email newEmail = new Email(email, remetente);
        newEmail.setCriadoEm(LocalDateTime.now());
        emailRepository.save(newEmail);
    }
}
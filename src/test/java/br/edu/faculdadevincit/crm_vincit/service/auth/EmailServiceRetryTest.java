package br.edu.faculdadevincit.crm_vincit.service.auth;

import br.edu.faculdadevincit.crm_vincit.repository.EmailRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Cobre o item de alta prioridade da auditoria (retry ausente nas integrações externas):
 * EmailService.sendEmail (usado por PasswordSendService para o e-mail de recuperação de senha)
 * agora tenta de novo em falha transitória de SMTP em vez de falhar na primeira tentativa. Precisa
 * de contexto Spring real (@EnableRetry + proxy AOP), um teste com Mockito puro não exercitaria o
 * retry, só o método em si.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EmailServiceRetryTest.RetryTestConfig.class, EmailService.class})
class EmailServiceRetryTest {

    @Configuration
    @EnableRetry
    static class RetryTestConfig {
    }

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private EmailRepository emailRepository;

    @Test
    void sendEmail_falhaTransitoriaDuasVezes_terceiraTentativaTemSucesso() {
        doThrow(new MailSendException("timeout SMTP"))
                .doThrow(new MailSendException("timeout SMTP"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailService.sendEmail("cliente@teste.com", "Assunto", "Corpo"))
                .doesNotThrowAnyException();

        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_falhaPersistente_esgotaAsTresTentativasEPropagaAExcecao() {
        doThrow(new MailSendException("SMTP fora do ar"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("cliente@teste.com", "Assunto", "Corpo"))
                .isInstanceOf(MailSendException.class);

        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }
}

package com.juridiqsystem.crm.service.auth;

import com.juridiqsystem.crm.repository.EmailRepository;
import com.juridiqsystem.crm.repository.UsuarioRepository;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobre o item de alta prioridade da auditoria (retry ausente nas integrações externas):
 * EmailService.sendEmail (usado por PasswordSendService para o e-mail de recuperação de senha)
 * tenta de novo em falha transitória da API do Resend em vez de falhar na primeira tentativa. Precisa
 * de contexto Spring real (@EnableRetry + proxy AOP), um teste com Mockito puro não exercitaria o
 * retry, só o método em si.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EmailServiceRetryTest.RetryTestConfig.class, EmailService.class})
@TestPropertySource(properties = {"resend.api-key=re_test", "resend.from=Teste <teste@example.com>"})
class EmailServiceRetryTest {

    @Configuration
    @EnableRetry
    static class RetryTestConfig {
    }

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private Resend resend;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private EmailRepository emailRepository;

    private Emails mockEmails() {
        Emails mocked = org.mockito.Mockito.mock(Emails.class);
        when(resend.emails()).thenReturn(mocked);
        return mocked;
    }

    @Test
    void sendEmail_falhaTransitoriaDuasVezes_terceiraTentativaTemSucesso() throws ResendException {
        Emails emails = mockEmails();
        doThrow(new ResendException("timeout"))
                .doThrow(new ResendException("timeout"))
                .doReturn(null)
                .when(emails).send(any(CreateEmailOptions.class));

        assertThatCode(() -> emailService.sendEmail("cliente@teste.com", "Assunto", "Corpo"))
                .doesNotThrowAnyException();

        verify(emails, times(3)).send(any(CreateEmailOptions.class));
    }

    @Test
    void sendEmail_falhaPersistente_esgotaAsTresTentativasEPropagaAExcecao() throws ResendException {
        Emails emails = mockEmails();
        doThrow(new ResendException("API do Resend fora do ar"))
                .when(emails).send(any(CreateEmailOptions.class));

        assertThatThrownBy(() -> emailService.sendEmail("cliente@teste.com", "Assunto", "Corpo"))
                .isInstanceOf(ResendException.class);

        verify(emails, times(3)).send(any(CreateEmailOptions.class));
    }
}

package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.model.EmpresaTwilioConfig;
import com.juridiqsystem.crm.model.dtos.EmpresaTwilioConfigResponseDTO;
import com.juridiqsystem.crm.model.dtos.EmpresaTwilioConfigUpdateDTO;
import com.juridiqsystem.crm.repository.EmpresaTwilioConfigRepository;
import com.juridiqsystem.crm.service.exceptions.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Painel "Integração Twilio" (/master/empresas/{empresaId}/twilio-config): cobre que o Auth Token
 * nunca é retornado em texto puro, que editar sem informar um novo token mantém o já salvo, que
 * criar pela primeira vez sem token é rejeitado, e que o cache de TwilioRestClient
 * (TwilioClientProvider) é sempre invalidado após qualquer alteração — senão o próximo envio
 * poderia usar credenciais desatualizadas até o TTL do cache expirar.
 */
@ExtendWith(MockitoExtension.class)
class EmpresaTwilioConfigServiceTest {

    @Mock
    private EmpresaTwilioConfigRepository repository;

    @Mock
    private TwilioClientProvider twilioClientProvider;

    @Mock
    private SecurityLogger securityLogger;

    @InjectMocks
    private EmpresaTwilioConfigService service;

    @AfterEach
    void limpaTenantContext() {
        TenantContext.clear();
    }

    private EmpresaTwilioConfigUpdateDTO dto(String accountSid, String authToken, String whatsappNumber, boolean enabled) {
        EmpresaTwilioConfigUpdateDTO dto = new EmpresaTwilioConfigUpdateDTO();
        dto.setAccountSid(accountSid);
        dto.setAuthToken(authToken);
        dto.setWhatsappNumber(whatsappNumber);
        dto.setEnabled(enabled);
        return dto;
    }

    @Test
    void salvarParaEmpresa_primeiraConfiguracaoSemAuthToken_lancaConflict() {
        when(repository.findByEmpresaId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvarParaEmpresa(1L, dto("AC1", "", "+10000000001", true)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void salvarParaEmpresa_primeiraConfiguracaoComAuthToken_salvaEInvalidaCache() {
        when(repository.findByEmpresaId(1L)).thenReturn(Optional.empty());
        when(repository.save(any(EmpresaTwilioConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmpresaTwilioConfigResponseDTO resultado = service.salvarParaEmpresa(1L, dto("AC1", "token-novo", "+10000000001", true));

        assertThat(resultado.isConfigurado()).isTrue();
        verify(twilioClientProvider).invalidate(1L);
    }

    @Test
    void salvarParaEmpresa_edicaoSemAuthToken_mantemTokenJaSalvo() {
        EmpresaTwilioConfig existente = new EmpresaTwilioConfig();
        existente.setId(10L);
        existente.setEmpresaId(1L);
        existente.setAccountSid("AC_ANTIGO");
        existente.setAuthToken("token-ja-salvo");
        existente.setWhatsappNumber("+10000000001");
        existente.setEnabled(true);
        when(repository.findByEmpresaId(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any(EmpresaTwilioConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.salvarParaEmpresa(1L, dto("AC_NOVO", "", "+10000000001", true));

        ArgumentCaptor<EmpresaTwilioConfig> captor = ArgumentCaptor.forClass(EmpresaTwilioConfig.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAuthToken()).isEqualTo("token-ja-salvo");
        assertThat(captor.getValue().getAccountSid()).isEqualTo("AC_NOVO");
        verify(twilioClientProvider).invalidate(1L);
    }

    @Test
    void salvarParaEmpresa_numeroWhatsappJaUsadoPorOutraEmpresa_lancaConflict() {
        when(repository.findByEmpresaId(1L)).thenReturn(Optional.empty());
        when(repository.save(any(EmpresaTwilioConfig.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.salvarParaEmpresa(1L, dto("AC1", "token", "+10000000099", true)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void buscarParaEmpresa_nuncaRetornaAuthTokenEmTextoPuro() {
        EmpresaTwilioConfig config = new EmpresaTwilioConfig();
        config.setEmpresaId(1L);
        config.setAccountSid("AC1");
        config.setAuthToken("segredo-super-sensivel-1234");
        config.setWhatsappNumber("+10000000001");
        config.setEnabled(true);
        when(repository.findByEmpresaId(1L)).thenReturn(Optional.of(config));

        EmpresaTwilioConfigResponseDTO resultado = service.buscarParaEmpresa(1L);

        assertThat(resultado.getAuthTokenMascarado()).isNotEqualTo("segredo-super-sensivel-1234");
        assertThat(resultado.getAuthTokenMascarado()).doesNotContain("segredo-super-sensivel");
        assertThat(resultado.getAuthTokenMascarado()).endsWith("1234");
    }

    @Test
    void buscarParaEmpresa_semConfiguracao_retornaConfiguradoFalso() {
        when(repository.findByEmpresaId(1L)).thenReturn(Optional.empty());

        EmpresaTwilioConfigResponseDTO resultado = service.buscarParaEmpresa(1L);

        assertThat(resultado.isConfigurado()).isFalse();
        assertThat(resultado.getAuthTokenMascarado()).isNull();
    }
}

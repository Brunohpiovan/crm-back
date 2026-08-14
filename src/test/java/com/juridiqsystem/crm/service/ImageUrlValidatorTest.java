package com.juridiqsystem.crm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Cobre "nunca aceitar qualquer domínio arbitrário de imagem" (avatar/logo/foto de grupo enviados
 * como URL, sem upload de arquivo) — ver UsuarioService/EmpresaService/ParticipanteService/
 * ChatGrupoService, que agora recusam qualquer valor que não passe por aqui.
 */
@ExtendWith(MockitoExtension.class)
class ImageUrlValidatorTest {

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private ImageUrlValidator imageUrlValidator;

    @Test
    void nuloOuEmBranco_sempreEhPermitido() {
        assertThat(imageUrlValidator.isPermitida(null)).isTrue();
        assertThat(imageUrlValidator.isPermitida("")).isTrue();
        assertThat(imageUrlValidator.isPermitida("   ")).isTrue();
    }

    @Test
    void assetPadrao_ehPermitido() {
        assertThat(imageUrlValidator.isPermitida("assets/img/avatar/padrao.jpeg")).isTrue();
    }

    @Test
    void urlDoProprioBucketS3_ehPermitida() {
        when(s3Service.getBaseUrl()).thenReturn("https://meu-bucket.s3.amazonaws.com");

        assertThat(imageUrlValidator.isPermitida("https://meu-bucket.s3.amazonaws.com/user-avatar/fotopic")).isTrue();
    }

    @Test
    void dominioArbitrario_semAllowlistConfigurada_ehRejeitado() {
        when(s3Service.getBaseUrl()).thenReturn("https://meu-bucket.s3.amazonaws.com");
        ReflectionTestUtils.setField(imageUrlValidator, "allowedDomainsRaw", "");

        assertThat(imageUrlValidator.isPermitida("https://evil.example.com/tracking.png")).isFalse();
    }

    @Test
    void dominioNaAllowlist_ehPermitido() {
        when(s3Service.getBaseUrl()).thenReturn("https://meu-bucket.s3.amazonaws.com");
        ReflectionTestUtils.setField(imageUrlValidator, "allowedDomainsRaw", "cdn.minhaempresa.com");

        assertThat(imageUrlValidator.isPermitida("https://cdn.minhaempresa.com/logo.png")).isTrue();
        assertThat(imageUrlValidator.isPermitida("https://sub.cdn.minhaempresa.com/logo.png")).isTrue();
        assertThat(imageUrlValidator.isPermitida("https://cdn.minhaempresa.com.evil.com/logo.png")).isFalse();
    }

    @Test
    void esquemaNaoHttp_ehRejeitadoMesmoComHostNaAllowlist() {
        when(s3Service.getBaseUrl()).thenReturn("https://meu-bucket.s3.amazonaws.com");
        ReflectionTestUtils.setField(imageUrlValidator, "allowedDomainsRaw", "cdn.minhaempresa.com");

        assertThat(imageUrlValidator.isPermitida("javascript:alert(1)")).isFalse();
        assertThat(imageUrlValidator.isPermitida("file:///etc/passwd")).isFalse();
    }
}

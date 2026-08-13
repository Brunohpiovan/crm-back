package com.juridiqsystem.crm.infra.security.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cobre a criptografia em repouso do access token da integração WhatsApp (Meta) — mesmo converter
 * já usado antes para o Auth Token da Twilio, agora reaproveitado por WhatsAppIntegration.accessToken.
 */
class EncryptedStringConverterTest {

    private static final String BASE64_KEY = "W66UXpccyv8nGOevei+KSVDKqK2Rc5EL5sF1lYi9UMc=";

    private final EncryptedStringConverter converter = new EncryptedStringConverter(BASE64_KEY);

    @Test
    void cifraEDecifraDeVoltaParaOValorOriginal() {
        String accessToken = "EAAG_valor_de_access_token_bem_longo_da_meta_123456";

        String ciphertext = converter.convertToDatabaseColumn(accessToken);
        String plaintext = converter.convertToEntityAttribute(ciphertext);

        assertThat(ciphertext).isNotEqualTo(accessToken);
        assertThat(plaintext).isEqualTo(accessToken);
    }

    @Test
    void doisValoresIguaisGeramCiphertextsDiferentes() {
        String accessToken = "mesmo-token";

        String ciphertext1 = converter.convertToDatabaseColumn(accessToken);
        String ciphertext2 = converter.convertToDatabaseColumn(accessToken);

        assertThat(ciphertext1).isNotEqualTo(ciphertext2);
    }

    @Test
    void nuloPermaneceNuloNosDoisSentidos() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void payloadAdulteradoFalhaAoDecifrar() {
        String ciphertext = converter.convertToDatabaseColumn("valor-original");
        byte[] bytes = Base64.getDecoder().decode(ciphertext);
        bytes[bytes.length - 1] ^= 0x01; // corrompe o último byte do tag de autenticação GCM
        String adulterado = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(adulterado))
                .isInstanceOf(IllegalStateException.class);
    }
}

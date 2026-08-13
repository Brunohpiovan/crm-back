package com.juridiqsystem.crm.infra.whatsapp;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class MetaWebhookSignatureValidatorTest {

    private final MetaWebhookSignatureValidator validator = new MetaWebhookSignatureValidator();

    private String sign(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void aceitaAssinaturaValida() throws Exception {
        String body = "{\"object\":\"whatsapp_business_account\"}";
        String secret = "app-secret-de-teste";
        String signature = sign(body, secret);

        assertThat(validator.isValid(body, signature, secret)).isTrue();
    }

    @Test
    void rejeitaAssinaturaDeOutroSegredo() throws Exception {
        String body = "{\"object\":\"whatsapp_business_account\"}";
        String signature = sign(body, "outro-segredo");

        assertThat(validator.isValid(body, signature, "app-secret-de-teste")).isFalse();
    }

    @Test
    void rejeitaCorpoAdulterado() throws Exception {
        String secret = "app-secret-de-teste";
        String signature = sign("{\"a\":1}", secret);

        assertThat(validator.isValid("{\"a\":2}", signature, secret)).isFalse();
    }

    @Test
    void rejeitaHeaderSemPrefixoSha256() {
        assertThat(validator.isValid("{}", "sem-prefixo-valido", "segredo")).isFalse();
    }

    @Test
    void rejeitaHeaderAusente() {
        assertThat(validator.isValid("{}", null, "segredo")).isFalse();
    }
}

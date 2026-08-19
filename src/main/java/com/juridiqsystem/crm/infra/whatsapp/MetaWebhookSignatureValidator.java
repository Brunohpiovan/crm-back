package com.juridiqsystem.crm.infra.whatsapp;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Valida a assinatura do webhook da Meta (header "X-Hub-Signature-256: sha256=&lt;hex hmac&gt;",
 * calculado pela Meta com HMAC-SHA256 sobre o corpo cru da requisição usando o App Secret) —
 * substitui com.twilio.security.RequestValidator (HMAC-SHA1 por conta). Diferente do modelo
 * Twilio (um Auth Token por empresa), aqui o App Secret é um único segredo do App do SaaS: a
 * assinatura é validada ANTES de qualquer resolução de tenant, o que é mais simples e mais seguro
 * que o fluxo antigo.
 */
@Component
public class MetaWebhookSignatureValidator {

    private static final String PREFIX = "sha256=";

    public boolean isValid(String rawBody, String signatureHeader, String appSecret) {
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX) || appSecret == null || appSecret.isBlank()) {
            return false;
        }
        String expectedHex = signatureHeader.substring(PREFIX.length()).trim();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    expectedHex.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}

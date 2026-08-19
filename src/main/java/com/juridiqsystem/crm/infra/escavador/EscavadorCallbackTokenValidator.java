package com.juridiqsystem.crm.infra.escavador;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Autentica o webhook público /webhooks/escavador/callback. Mesmo princípio de
 * {@link com.juridiqsystem.crm.infra.whatsapp.MetaWebhookSignatureValidator} (segredo único do
 * SaaS, validado ANTES de qualquer resolução de tenant), mas com segredo compartilhado em vez de
 * HMAC: a Escavador não assina o corpo do callback — ela envia, no header {@code Authorization},
 * um token que nós mesmos geramos no painel da API
 * (https://api.escavador.com/v2/docs/callbacks). Se a Escavador passar a oferecer assinatura
 * HMAC do corpo, ela deve virar a validação primária e este token continua como camada extra.
 *
 * <p>O token também é aceito via query param {@code ?token=} para o caso de o painel só permitir
 * embutir o segredo na própria URL de callback. O header é preferido: query strings aparecem em
 * log de acesso, proxy e Referer.</p>
 */
@Component
public class EscavadorCallbackTokenValidator {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * @param authorizationHeader conteúdo do header Authorization do callback (com ou sem o
     *                            prefixo "Bearer ").
     * @param tokenQueryParam     token vindo de {@code ?token=}, quando usado.
     * @param tokenConfigurado    segredo cadastrado no painel da Escavador.
     * @return true apenas se um dos dois valores recebidos casar exatamente com o configurado.
     */
    public boolean isValid(String authorizationHeader, String tokenQueryParam, String tokenConfigurado) {
        if (tokenConfigurado == null || tokenConfigurado.isBlank()) {
            return false;
        }
        return matches(semPrefixoBearer(authorizationHeader), tokenConfigurado)
                || matches(tokenQueryParam, tokenConfigurado);
    }

    private String semPrefixoBearer(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String valor = authorizationHeader.trim();
        return valor.startsWith(BEARER_PREFIX) ? valor.substring(BEARER_PREFIX.length()).trim() : valor;
    }

    /** MessageDigest.isEqual compara em tempo constante — evita descobrir o token byte a byte. */
    private boolean matches(String recebido, String esperado) {
        if (recebido == null || recebido.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                recebido.getBytes(StandardCharsets.UTF_8),
                esperado.getBytes(StandardCharsets.UTF_8));
    }
}

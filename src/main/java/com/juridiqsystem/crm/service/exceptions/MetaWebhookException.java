package com.juridiqsystem.crm.service.exceptions;

/** Verificação de assinatura (X-Hub-Signature-256) ou verify token do webhook da Meta falhou. */
public class MetaWebhookException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MetaWebhookException(String message) {
        super(message);
    }
}

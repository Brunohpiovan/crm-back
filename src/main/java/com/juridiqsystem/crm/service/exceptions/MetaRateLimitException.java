package com.juridiqsystem.crm.service.exceptions;

/** Rate limit da Graph API (ou do nosso próprio limitador) excedido para envio de mensagens. */
public class MetaRateLimitException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MetaRateLimitException(String message) {
        super(message);
    }
}

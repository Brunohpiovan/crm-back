package com.juridiqsystem.crm.service.exceptions;

/** Token da Meta ausente, expirado ou revogado — a empresa precisa reconectar o WhatsApp. */
public class MetaAuthenticationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MetaAuthenticationException(String message) {
        super(message);
    }
}

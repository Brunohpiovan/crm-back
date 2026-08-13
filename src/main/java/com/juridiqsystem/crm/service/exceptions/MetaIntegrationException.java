package com.juridiqsystem.crm.service.exceptions;

/** Falha transitória (timeout, erro de rede, 5xx) ao chamar a Graph API — candidata a retry. */
public class MetaIntegrationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MetaIntegrationException(String message) {
        super(message);
    }

    public MetaIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}

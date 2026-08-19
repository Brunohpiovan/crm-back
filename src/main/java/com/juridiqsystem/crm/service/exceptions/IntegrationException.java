package com.juridiqsystem.crm.service.exceptions;

public class IntegrationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IntegrationException(String message) {
        super(message);
    }

    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.juridiqsystem.crm.service.exceptions;

/** Falha ao enviar uma mensagem (texto/mídia) através da Graph API — erro de negócio, não transitório. */
public class MetaMessageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MetaMessageException(String message) {
        super(message);
    }

    public MetaMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}

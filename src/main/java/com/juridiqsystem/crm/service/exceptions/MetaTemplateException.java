package com.juridiqsystem.crm.service.exceptions;

/** Template inexistente, não aprovado, ou parâmetros incompatíveis com o template cadastrado na Meta. */
public class MetaTemplateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MetaTemplateException(String message) {
        super(message);
    }
}

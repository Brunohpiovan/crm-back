package br.edu.faculdadevincit.crm_vincit.service.exceptions;

public class ConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}

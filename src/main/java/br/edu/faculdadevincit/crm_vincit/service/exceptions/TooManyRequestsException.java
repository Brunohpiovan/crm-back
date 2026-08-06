package br.edu.faculdadevincit.crm_vincit.service.exceptions;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}

package br.edu.faculdadevincit.crm_vincit.service.exceptions;

public class UsuarioBloqueadoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UsuarioBloqueadoException(String message) {
        super(message);
    }
}

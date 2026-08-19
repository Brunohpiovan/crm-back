package com.juridiqsystem.crm.service.exceptions;

/**
 * Erro HTTP (4xx/5xx) retornado pela Escavador. Estende IntegrationException (não um novo handler
 * paralelo em ResourceExceptionHandler) para reaproveitar o mapeamento já existente para 502 —
 * ver CONVENTIONS.md §5. statusHttp preserva o status original (0 quando a falha foi de
 * conectividade, sem resposta HTTP) para chamadores que precisam diferenciar "não encontrado" (404)
 * de outros erros, sem re-parsear a mensagem — ver ProcessoService.tentarObterStatusResumoIa.
 */
public class EscavadorApiException extends IntegrationException {
    private static final long serialVersionUID = 1L;

    private final int statusHttp;

    public EscavadorApiException(String message) {
        this(message, 0);
    }

    public EscavadorApiException(String message, int statusHttp) {
        super(message);
        this.statusHttp = statusHttp;
    }

    public EscavadorApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusHttp = 0;
    }

    public int getStatusHttp() {
        return statusHttp;
    }
}

package com.juridiqsystem.crm.service.exceptions;

import com.juridiqsystem.crm.infra.security.logging.SecurityEventType;
import com.juridiqsystem.crm.infra.security.logging.SecurityLogger;
import com.juridiqsystem.crm.model.dtos.ApiResponse;
import com.juridiqsystem.crm.service.auth.ClientInfoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ResourceExceptionHandler {

    @Autowired
    private SecurityLogger securityLogger;

    @Autowired
    private ClientInfoService clientInfoService;

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> dataIntegrityViolationException(DataIntegrityViolationException ex, HttpServletRequest request){
        StandardError error = new StandardError(System.currentTimeMillis(), HttpStatus.BAD_REQUEST.value(),"Violacao de dados", ex.getMessage(), request.getRequestURI() );

        return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> jpaDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request){
        log.warn("Violacao de integridade de dados na camada de persistencia", ex);
        StandardError error = new StandardError(System.currentTimeMillis(), HttpStatus.BAD_REQUEST.value(),
                "Violacao de dados", "Nao foi possivel concluir a operacao devido a uma restricao de integridade dos dados.", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> resourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request){
        StandardError error = new StandardError(System.currentTimeMillis(), HttpStatus.NOT_FOUND.value(),
                "Recurso nao encontrado", ex.getMessage(), request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<StandardError> conflictException(ConflictException ex, HttpServletRequest request){
        StandardError error = new StandardError(System.currentTimeMillis(), HttpStatus.CONFLICT.value(),
                "Conflito", ex.getMessage(), request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<StandardError> integrationException(IntegrationException ex, HttpServletRequest request){
        log.error("Falha ao integrar com servico externo", ex);
        StandardError error = new StandardError(System.currentTimeMillis(), HttpStatus.BAD_GATEWAY.value(),
                "Erro de integracao", "Nao foi possivel concluir a operacao devido a uma falha em um servico externo.", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardError> cpfValidationErrors(ConstraintViolationException ex, HttpServletRequest request){

        ValidationError errors = new ValidationError(System.currentTimeMillis(), HttpStatus.BAD_REQUEST.value(),
                "Validation Error", "Erro na validação dos campos", request.getRequestURI());


        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.addError(fieldName, errorMessage);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        log.error("Erro inesperado nao tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro interno no servidor. Tente novamente mais tarde.");
    }

    /**
     * Trata ResponseStatusException com o status/motivo que o service de fato pediu (ex.: 404,
     * 409), em vez de cair no handler genérico de RuntimeException abaixo — que achatava tudo
     * pra 400 com o toString() cru da exceção (ex.: "404 NOT_FOUND \"Cadencia não encontrada\"").
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<StandardError> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatusCode statusCode = ex.getStatusCode();
        StandardError error = new StandardError(System.currentTimeMillis(), statusCode.value(),
                HttpStatus.resolve(statusCode.value()) != null ? HttpStatus.resolve(statusCode.value()).getReasonPhrase() : "Erro",
                ex.getReason() != null ? ex.getReason() : "Erro ao processar a requisição", request.getRequestURI());
        return ResponseEntity.status(statusCode).body(error);
    }

    /**
     * Catch-all pra RuntimeException de negócio não mapeada num tipo mais específico (ex.:
     * ".orElseThrow(() -> new RuntimeException(\"X não encontrado\"))" espalhado pelos services).
     * A mensagem aqui já é sempre um texto de negócio controlado, nunca stack trace/SQL — mas a
     * resposta agora é JSON estruturado (StandardError) em vez de texto puro, pra manter o
     * mesmo formato dos outros handlers.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<StandardError> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        StandardError error = new StandardError(System.currentTimeMillis(), HttpStatus.BAD_REQUEST.value(),
                "Requisição inválida", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(UsuarioBloqueadoException.class)
    public ResponseEntity<ApiResponse> handleUsuarioBloqueado(UsuarioBloqueadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse(ex.getMessage()));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponse> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest request) {
        // /auth/login já loga o próprio RATE_LIMIT_TRIGGERED com mais contexto (login/empresa
        // tentados) antes de lançar a exceção — evita duplicar o evento aqui.
        if (!"/auth/login".equals(request.getRequestURI())) {
            securityLogger.log(SecurityEventType.RATE_LIMIT_TRIGGERED, ex.getMessage(), currentActor(),
                    clientInfoService.getClientIp(request), request.getRequestURI());
        }
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiResponse(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        securityLogger.log(SecurityEventType.ACCESS_DENIED, ex.getMessage(), currentActor(),
                clientInfoService.getClientIp(request), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse(ex.getMessage()));
    }

    private String currentActor() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : null;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(errors);
    }

}

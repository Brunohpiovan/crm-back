package br.edu.faculdadevincit.crm_vincit.service.exceptions;

import br.edu.faculdadevincit.crm_vincit.model.dtos.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceExceptionHandlerTest {

    private final ResourceExceptionHandler handler = new ResourceExceptionHandler();

    @Mock
    private HttpServletRequest request;

    @Test
    void dataIntegrityViolationException_retorna400ComStandardError() {
        when(request.getRequestURI()).thenReturn("/usuario");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("E-mail já cadastrado no sistema.");

        ResponseEntity<StandardError> resposta = handler.dataIntegrityViolationException(ex, request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().getError()).isEqualTo("Violacao de dados");
        assertThat(resposta.getBody().getMessage()).isEqualTo("E-mail já cadastrado no sistema.");
        assertThat(resposta.getBody().getPath()).isEqualTo("/usuario");
    }

    @Test
    void illegalArgumentException_retorna400ComApiResponse() {
        IllegalArgumentException ex = new IllegalArgumentException("Token inválido ou expirado.");

        ResponseEntity<ApiResponse> resposta = handler.handleIllegalArgumentException(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message()).isEqualTo("Token inválido ou expirado.");
    }

    @Test
    void usuarioBloqueadoException_retorna403ComApiResponse() {
        UsuarioBloqueadoException ex = new UsuarioBloqueadoException("Você não tem permissão para acessar o sistema.");

        ResponseEntity<ApiResponse> resposta = handler.handleUsuarioBloqueado(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody().message()).isEqualTo("Você não tem permissão para acessar o sistema.");
    }

    @Test
    void accessDeniedException_retorna403ComApiResponse() {
        AccessDeniedException ex = new AccessDeniedException("Você não tem permissão para acessar este usuário.");

        ResponseEntity<ApiResponse> resposta = handler.handleAccessDenied(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody().message()).isEqualTo("Você não tem permissão para acessar este usuário.");
    }

    @Test
    void runtimeExceptionGenerica_retorna400ComMensagemEmTexto() {
        RuntimeException ex = new RuntimeException("Protocolo nao encontrado");

        ResponseEntity<String> resposta = handler.handleRuntimeException(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isEqualTo("Protocolo nao encontrado");
    }

    @Test
    void exceptionGenerica_retorna500SemVazarStackTrace() {
        Exception ex = new IllegalStateException("Falha interna inesperada");

        ResponseEntity<?> resposta = handler.handleGenericException(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().toString()).doesNotContain("Falha interna inesperada");
        assertThat(resposta.getBody().toString()).isEqualTo("Erro interno no servidor. Tente novamente mais tarde.");
    }

    @Test
    void methodArgumentNotValidException_retorna400ComMapaDeCamposInvalidos() {
        FieldError fieldError = new FieldError("usuarioDTO", "cep", "O campo CEP é requerido.");
        BindingResult bindingResult = org.mockito.Mockito.mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> resposta = handler.handleValidationErrors(ex);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("cep", "O campo CEP é requerido.");
    }

    @Test
    void resourceNotFoundException_retorna404ComStandardError() {
        when(request.getRequestURI()).thenReturn("/oportunidade/99");
        ResourceNotFoundException ex = new ResourceNotFoundException("Oportunidade com id 99 nao encontrada");

        ResponseEntity<StandardError> resposta = handler.resourceNotFoundException(ex, request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().getMessage()).isEqualTo("Oportunidade com id 99 nao encontrada");
        assertThat(resposta.getBody().getPath()).isEqualTo("/oportunidade/99");
    }

    @Test
    void conflictException_retorna409ComStandardError() {
        when(request.getRequestURI()).thenReturn("/tag");
        ConflictException ex = new ConflictException("Ja existe uma tag com esse nome.");

        ResponseEntity<StandardError> resposta = handler.conflictException(ex, request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().getMessage()).isEqualTo("Ja existe uma tag com esse nome.");
    }

    @Test
    void integrationException_retorna502SemVazarDetalheDoServicoExterno() {
        when(request.getRequestURI()).thenReturn("/oportunidade");
        IntegrationException ex = new IntegrationException("Falha ao enviar arquivo", new RuntimeException("S3 timeout interno"));

        ResponseEntity<StandardError> resposta = handler.integrationException(ex, request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(resposta.getBody().getMessage()).doesNotContain("S3 timeout interno");
    }

    @Test
    void jpaDataIntegrityViolationException_retorna400SemVazarMensagemDeDriver() {
        when(request.getRequestURI()).thenReturn("/usuario");
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException(
                        "could not execute statement; SQL [insert into usuario]; constraint [uk_usuario_email]");

        ResponseEntity<StandardError> resposta = handler.jpaDataIntegrityViolationException(ex, request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().getMessage()).doesNotContain("SQL");
        assertThat(resposta.getBody().getMessage()).doesNotContain("constraint");
    }
}

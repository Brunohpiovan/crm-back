package com.juridiqsystem.crm.infra.escavador;

import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.service.exceptions.EscavadorApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.Map;

/**
 * Camada centralizada de comunicação com a Escavador Business API. Nenhuma outra classe do projeto
 * deve montar uma URL "api.escavador.com/..." ou usar RestClient diretamente para falar com a
 * Escavador — tudo passa por aqui (mesmo padrão de MetaWhatsAppClient). Duas instâncias internas de
 * RestClient, uma por versão de API (v1/v2), ambas com auth Bearer PAT — ver
 * docs/integrations/escavador-api.md §Authentication.
 */
@Slf4j
@Component
public class EscavadorClient {

    private static final String V1_BASE_URL = "https://api.escavador.com/api/v1";
    private static final String V2_BASE_URL = "https://api.escavador.com/api/v2";
    private static final String CREDITOS_HEADER = "Creditos-Utilizados";

    private final EscavadorApiProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final RestClient clientV1;
    private final RestClient clientV2;

    public EscavadorClient(EscavadorApiProperties properties, ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());
        this.clientV1 = RestClient.builder().baseUrl(V1_BASE_URL).requestFactory(requestFactory).build();
        this.clientV2 = RestClient.builder().baseUrl(V2_BASE_URL).requestFactory(requestFactory).build();
    }

    @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 4,
            backoff = @Backoff(delay = 500, multiplier = 2, random = true))
    public <T> EscavadorResponse<T> get(EscavadorApiVersion versao, String path, Map<String, String> query, Class<T> tipoResposta) {
        return execute(versao, HttpMethod.GET, path, query, null, tipoResposta);
    }

    @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 4,
            backoff = @Backoff(delay = 500, multiplier = 2, random = true))
    public <T> EscavadorResponse<T> post(EscavadorApiVersion versao, String path, Object corpo, Class<T> tipoResposta) {
        return execute(versao, HttpMethod.POST, path, null, corpo, tipoResposta);
    }

    @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 4,
            backoff = @Backoff(delay = 500, multiplier = 2, random = true))
    public <T> EscavadorResponse<T> delete(EscavadorApiVersion versao, String path, Class<T> tipoResposta) {
        return execute(versao, HttpMethod.DELETE, path, null, null, tipoResposta);
    }

    /**
     * Download binário (ex.: PDF de documento) — único método que não passa por
     * {@code .retrieve().toEntity(tipoResposta)} com o Accept JSON padrão de {@link #authHeaders},
     * já que o corpo da resposta nunca é JSON aqui. Mesmos aspectos dos demais métodos (auth
     * Bearer, retry em 429, publicação de EscavadorRequisicaoRealizadaEvent).
     */
    @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 4,
            backoff = @Backoff(delay = 500, multiplier = 2, random = true))
    public EscavadorResponse<byte[]> getBinario(EscavadorApiVersion versao, String path) {
        return execute(versao, HttpMethod.GET, path, null, null, byte[].class, this::authHeadersBinario);
    }

    private <T> EscavadorResponse<T> execute(EscavadorApiVersion versao, HttpMethod metodo, String path,
                                              Map<String, String> query, Object corpo, Class<T> tipoResposta) {
        return execute(versao, metodo, path, query, corpo, tipoResposta, this::authHeaders);
    }

    private <T> EscavadorResponse<T> execute(EscavadorApiVersion versao, HttpMethod metodo, String path,
                                              Map<String, String> query, Object corpo, Class<T> tipoResposta,
                                              java.util.function.Consumer<HttpHeaders> headersCustomizer) {
        RestClient client = versao == EscavadorApiVersion.V1 ? clientV1 : clientV2;
        try {
            RestClient.RequestBodySpec spec = client.method(metodo)
                    .uri(uriBuilder -> buildUri(uriBuilder, path, query))
                    .headers(headersCustomizer::accept);
            if (corpo != null) {
                spec.body(corpo);
            }
            ResponseEntity<T> resposta = spec.retrieve().toEntity(tipoResposta);
            int custo = extrairCredito(resposta.getHeaders());
            publicarEvento(path, custo, true);
            return new EscavadorResponse<>(resposta.getBody(), custo, resposta.getStatusCode().value());
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw e; // deixa o @Retryable interceptar
        } catch (RestClientResponseException e) {
            int custo = extrairCredito(e.getResponseHeaders());
            publicarEvento(path, custo, false);
            throw mapError(e, path);
        } catch (ResourceAccessException e) {
            publicarEvento(path, 0, false);
            throw new EscavadorApiException("A API da Escavador está indisponível no momento.", e);
        }
    }

    private URI buildUri(UriBuilder uriBuilder, String path, Map<String, String> query) {
        uriBuilder.path(path);
        if (query != null) {
            query.forEach(uriBuilder::queryParam);
        }
        return uriBuilder.build();
    }

    private void authHeaders(HttpHeaders headers) {
        headers.setBearerAuth(properties.getToken());
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
    }

    private void authHeadersBinario(HttpHeaders headers) {
        headers.setBearerAuth(properties.getToken());
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_PDF, MediaType.ALL));
    }

    private int extrairCredito(HttpHeaders headers) {
        if (headers == null) {
            return 0;
        }
        String valor = headers.getFirst(CREDITOS_HEADER);
        if (valor == null || valor.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void publicarEvento(String endpoint, int custoCentavos, boolean sucesso) {
        Long empresaId = TenantContext.get();
        eventPublisher.publishEvent(new EscavadorRequisicaoRealizadaEvent(empresaId, endpoint, custoCentavos, sucesso));
    }

    /**
     * Traduz o erro cru da Escavador numa EscavadorApiException com mensagem de negócio — o corpo
     * cru da resposta nunca chega ao controller/frontend. 402 é "sem saldo" (ver docs §Balance).
     */
    private EscavadorApiException mapError(RestClientResponseException e, String path) {
        HttpStatusCode status = e.getStatusCode();
        log.warn("Erro retornado pela Escavador API: path={} status={} corpo={}", path, status.value(), e.getResponseBodyAsString());

        if (status.value() == 401) {
            return new EscavadorApiException("Autenticação com a Escavador falhou. Verifique o token configurado.", status.value());
        }
        if (status.value() == 402) {
            return new EscavadorApiException("Saldo de créditos insuficiente na conta Escavador.", status.value());
        }
        if (status.value() == 404) {
            return new EscavadorApiException("Processo ou recurso não encontrado na Escavador.", status.value());
        }
        if (status.value() == 422) {
            return new EscavadorApiException("Dados inválidos para a consulta na Escavador (ex.: número CNJ malformado).", status.value());
        }
        return new EscavadorApiException("Não foi possível concluir a operação com a Escavador no momento.", status.value());
    }
}

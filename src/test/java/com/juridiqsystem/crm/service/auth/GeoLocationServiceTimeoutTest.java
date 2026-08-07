package com.juridiqsystem.crm.service.auth;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * GeoLocationService.getGeoLocation não tinha timeout configurado no RestTemplate: um servidor
 * lento/travado prendia a thread da requisição de login indefinidamente. Este teste sobe um
 * servidor TCP que aceita a conexão mas nunca responde, e garante que a chamada retorna (com
 * null, via o catch já existente) dentro de um tempo limitado em vez de travar.
 */
class GeoLocationServiceTimeoutTest {

    @Test
    void getGeoLocation_comServidorQueNuncaResponde_retornaAposOTimeoutConfigurado() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int porta = serverSocket.getLocalPort();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try (Socket socket = serverSocket.accept()) {
                    // Aceita a conexão e nunca escreve nada de volta, simulando um servidor travado.
                    Thread.sleep(20_000);
                } catch (Exception ignored) {
                    // Encerrado quando o teste termina e fecha o ServerSocket.
                }
            });

            GeoLocationService geoLocationService = new GeoLocationService();
            ReflectionTestUtils.setField(geoLocationService, "apiUrl", "http://localhost:" + porta + "/");

            Map<String, Object> resultado = assertTimeoutPreemptively(
                    java.time.Duration.ofSeconds(8),
                    () -> geoLocationService.getGeoLocation("8.8.8.8")
            );

            assertThat(resultado).isNull();
            executor.shutdownNow();
        }
    }
}

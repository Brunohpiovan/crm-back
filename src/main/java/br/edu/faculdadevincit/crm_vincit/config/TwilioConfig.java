package br.edu.faculdadevincit.crm_vincit.config;

import com.twilio.http.NetworkHttpClient;
import com.twilio.http.TwilioRestClient;
import org.apache.http.client.config.RequestConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente Twilio reaproveitado (bean singleton) com timeouts explícitos. Antes, cada envio de
 * mensagem chamava {@code Twilio.init(...)} e usava o cliente HTTP global padrão do SDK, sem
 * nenhum timeout configurado — diferente do download de mídia recebida
 * (WhatsAppService.downloadMediaFromTwilio), que já tinha connect/read timeout definidos.
 */
@Configuration
public class TwilioConfig {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int SOCKET_TIMEOUT_MS = 15_000;

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Bean
    public TwilioRestClient twilioRestClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .build();
        NetworkHttpClient httpClient = new NetworkHttpClient(requestConfig);
        return new TwilioRestClient.Builder(accountSid, authToken)
                .httpClient(httpClient)
                .build();
    }
}

package com.juridiqsystem.crm.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GeoLocationService {

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    @Value("${geo.api.url}")
    private String apiUrl;

    public Map<String, Object> getGeoLocation(String ip) {
        try {
            RestTemplate restTemplate = new RestTemplate(criarRequestFactory());
            return restTemplate.getForObject(apiUrl + ip, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    private SimpleClientHttpRequestFactory criarRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);
        return requestFactory;
    }
}

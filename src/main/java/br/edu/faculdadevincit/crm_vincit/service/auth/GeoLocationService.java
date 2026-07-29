package br.edu.faculdadevincit.crm_vincit.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GeoLocationService {

    @Value("${geo.api.url}")
    private String apiUrl;

    public Map<String, Object> getGeoLocation(String ip) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.getForObject(apiUrl + ip, Map.class);
        } catch (Exception e) {
            return null;
        }
    }
}

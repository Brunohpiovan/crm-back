package br.edu.faculdadevincit.crm_vincit.service.auth;

import br.edu.faculdadevincit.crm_vincit.model.Acesso;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ClientInfoService {

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    @Value("${api.ip-location.url}")
    private String ipApiUrl;

    @Autowired
    private GeoLocationService geoLocationService;

    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getRemoteAddr();

        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip) || "0.0.0.0".equals(ip)) {
            ip = getPublicIp();
        }

        return ip;
    }

    public UserAgent getUserAgent(HttpServletRequest request) {
        return UserAgent.parseUserAgentString(request.getHeader("User-Agent"));
    }

    private String getPublicIp() {
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
            requestFactory.setReadTimeout(READ_TIMEOUT_MS);
            RestTemplate restTemplate = new RestTemplate(requestFactory);
            Map<String, Object> response = restTemplate.getForObject(ipApiUrl, Map.class);
            return (String) response.get("query");
        } catch (Exception e) {
            return "IP não encontrado";
        }
    }

    public Acesso createLogAcesso(Usuario usuario, HttpServletRequest request) {
        Acesso acesso = new Acesso();
        acesso.setUsuario(usuario);
        String ipUsuario = getClientIp(request);
        acesso.setEnderecoIp(ipUsuario);
        acesso.setData_acesso(LocalDateTime.now());

        UserAgent userAgent = getUserAgent(request);
        String versaoNavegador = userAgent.getBrowserVersion() != null ? userAgent.getBrowserVersion().getVersion() : "Desconhecido";
        acesso.setSistemaOperacional(userAgent.getOperatingSystem().getName());
        acesso.setNavegador(userAgent.getBrowser().getName());
        acesso.setVersaoNavegador(versaoNavegador);
        acesso.setTipoDispositivo(tipoDispositivoCaptura(userAgent));

        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            String idioma = acceptLanguage.split(",")[0];
            acesso.setIdiomaNavegador(idioma);
        }

        Map<String, Object> geoData = geoLocationService.getGeoLocation(ipUsuario);
        if (geoData != null) {
            acesso.setLocalizacao(
                    geoData.get("city") + ", " + geoData.get("regionName") + ", " + geoData.get("country"));
            acesso.setProvedorInternet((String) geoData.get("isp"));
            acesso.setFusoHorario((String) geoData.get("timezone"));
        }

        return acesso;
    }

    public String tipoDispositivoCaptura(UserAgent userAgent) {
        String tipoDispositivo = userAgent.getOperatingSystem().getDeviceType().getName();
        String tipoDispositivoPt;

        switch (tipoDispositivo) {
            case "Computer":
                tipoDispositivoPt = "Computador";
                break;
            case "Mobile":
                tipoDispositivoPt = "Celular";
                break;
            case "Tablet":
                tipoDispositivoPt = "Tablet";
                break;
            case "Game console":
                tipoDispositivoPt = "Console de jogo";
                break;
            case "Digital media receiver":
                tipoDispositivoPt = "Receptor de mídia digital";
                break;
            case "Wearable":
                tipoDispositivoPt = "Dispositivo vestível";
                break;
            case "Unknown":
            default:
                tipoDispositivoPt = "Desconhecido";
                break;
        }
        return tipoDispositivoPt;
    }

}

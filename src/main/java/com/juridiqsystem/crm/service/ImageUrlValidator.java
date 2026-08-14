package com.juridiqsystem.crm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Valida qualquer URL de imagem que o cliente possa mandar diretamente (sem upload de arquivo) —
 * avatar de usuário/participante, foto de grupo de chat, logo de empresa. Sem isso, o campo
 * aceitava qualquer string do cliente e persistia direto (ex.: UsuarioService.resolveUrlPicture),
 * violando "nunca aceitar qualquer domínio arbitrário" mesmo sem o backend chegar a baixar o
 * arquivo (o navegador de outros usuários da empresa é quem carregaria essa URL).
 *
 * Sempre permitidos: em branco/nulo (o chamador decide o que fazer — normalmente manter o valor
 * atual), o asset padrão do frontend, e qualquer URL dentro do próprio bucket S3 da aplicação
 * (gerada pelo próprio backend em upload). Qualquer outra URL só é aceita se o host bater com
 * ALLOWED_IMAGE_DOMAINS (vazio por padrão = nenhum domínio externo extra liberado).
 */
@Component
public class ImageUrlValidator {

    public static final String DEFAULT_AVATAR_PATH = "assets/img/avatar/padrao.jpeg";

    @Value("${app.allowed-image-domains:}")
    private String allowedDomainsRaw;

    @Autowired
    private S3Service s3Service;

    public boolean isPermitida(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        if (DEFAULT_AVATAR_PATH.equals(url)) {
            return true;
        }
        String baseUrl = s3Service.getBaseUrl();
        if (baseUrl != null && !baseUrl.isBlank() && url.startsWith(baseUrl)) {
            return true;
        }
        return hostPermitido(url);
    }

    private boolean hostPermitido(String url) {
        List<String> allowedDomains = parseAllowedDomains();
        if (allowedDomains.isEmpty()) {
            return false;
        }
        String host;
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            host = uri.getHost();
        } catch (Exception e) {
            return false;
        }
        if (host == null) {
            return false;
        }
        String hostLower = host.toLowerCase(Locale.ROOT);
        return allowedDomains.stream().anyMatch(domain ->
                hostLower.equals(domain) || hostLower.endsWith("." + domain));
    }

    private List<String> parseAllowedDomains() {
        if (allowedDomainsRaw == null || allowedDomainsRaw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedDomainsRaw.split(","))
                .map(String::trim)
                .map(d -> d.toLowerCase(Locale.ROOT))
                .filter(d -> !d.isEmpty())
                .toList();
    }
}

package com.juridiqsystem.crm.infra.security;

import com.juridiqsystem.crm.service.auth.ClientInfoService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Regras de origem para a area do usuario master (super-admin multi-empresa). Extraido do
 * AdminRouteGuardFilter porque a mesma decisao precisa ser tomada em dois momentos diferentes:
 *
 *  - No AdminRouteGuardFilter, barrando cada requisicao a /master/**;
 *  - No login (AuthenticationService), recusando de saida a emissao de um JWT de MASTER para uma
 *    origem nao autorizada — sem isso, o token era emitido normalmente e so os acessos seguintes
 *    eram barrados, o que dependia do frontend saber lidar com a rejeicao.
 *
 * As duas checagens sao independentes e cada uma so fica ativa se a env var correspondente estiver
 * configurada (vazio = desligada):
 *  - ADMIN_ROUTE_SECRET: header X-Admin-Route-Secret precisa bater exatamente (comparacao em tempo
 *    constante, para nao vazar o segredo por timing).
 *  - ADMIN_IP_ALLOWLIST: IP do cliente (ver ClientInfoService) precisa estar numa faixa liberada.
 *
 * O segredo e uma regra de ROTA, nao de sessao: e checado so em /master/**, nunca no login. Exigi-lo
 * no login obrigaria o frontend publico a carregar o segredo no bundle, onde ele deixaria de ser
 * segredo. A allowlist de IP, ao contrario, e propriedade da conexao e vale nos dois momentos.
 */
@Component
public class AdminAccessPolicy {

    private static final String SECRET_HEADER = "X-Admin-Route-Secret";

    @Value("${app.admin.route-secret:}")
    private String configuredSecret;

    @Value("${app.admin.ip-allowlist:}")
    private String ipAllowlistRaw;

    @Autowired
    private ClientInfoService clientInfoService;

    private CidrMatcherList ipAllowlist = CidrMatcherList.parse("", "ADMIN_IP_ALLOWLIST");

    @PostConstruct
    void init() {
        ipAllowlist = CidrMatcherList.parse(ipAllowlistRaw, "ADMIN_IP_ALLOWLIST");
    }

    /**
     * Motivo da recusa, ou null se a requisicao pode acessar a area master. Nunca deve ser devolvido
     * ao cliente — serve so para o SecurityLogger interno.
     */
    public String denialReason(HttpServletRequest request) {
        if (isSecretConfigured() && !secretMatches(request)) {
            return "Segredo de rota administrativa ausente ou invalido";
        }
        if (!isIpAllowed(request)) {
            return "IP fora da allowlist administrativa";
        }
        return null;
    }

    /** So a checagem de IP — usada no login, onde o segredo de rota nao se aplica. */
    public boolean isIpAllowed(HttpServletRequest request) {
        return ipAllowlist.isEmpty() || ipAllowlist.matches(clientInfoService.getClientIp(request));
    }

    private boolean isSecretConfigured() {
        return configuredSecret != null && !configuredSecret.isBlank();
    }

    private boolean secretMatches(HttpServletRequest request) {
        String provided = request.getHeader(SECRET_HEADER);
        if (provided == null || provided.isEmpty()) {
            return false;
        }
        byte[] a = provided.getBytes(StandardCharsets.UTF_8);
        byte[] b = configuredSecret.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}

package com.juridiqsystem.crm.infra.security;


import com.juridiqsystem.crm.model.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    /**
     * Subject é o publicId (UUID) do Usuario, não o id numérico interno nem o login — um JWT é
     * só assinado, não criptografado, então qualquer um pode decodificar o payload no navegador
     * (jwt.io, devtools); usar o id sequencial aqui vazaria exatamente o dado que o public_id
     * existe pra esconder. O claim empresaId é o que permite ao SecurityFilter popular o
     * TenantContext ANTES de buscar o Usuario no banco (Usuario é @TenantId, então sem o
     * tenant já resolvido a busca não encontraria nada) — fica como id numérico interno mesmo,
     * Empresa ainda não tem nenhum endpoint que a exponha (ver DASHBOARD.md/plano de multi-tenant).
     *
     * <p>Os claims de autorização são explícitos (`admin`, `master`, `cargoNome`, `permissoes`)
     * desde que cargo virou entidade por empresa: o antigo claim único `roles` carregava o nome
     * do enum UserRole, que não existe mais. Eles servem só para a UI decidir o que exibir — o
     * enforcement real continua sendo authority + SecurityConfiguration, recalculado a cada
     * requisição a partir do banco (ver Usuario.getAuthorities()), nunca a partir do token.</p>
     */
    public String generateToken(Usuario user){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String token = JWT.create()
                    .withIssuer("juridiqsystem")
                    .withSubject(user.getPublicId())
                    .withClaim("id", user.getPublicId())
                    .withClaim("empresaId", user.getEmpresaId())
                    .withClaim("nome",user.getNome())
                    .withClaim("admin", isAdministrador(user))
                    .withClaim("master", Boolean.TRUE.equals(user.getMaster()))
                    .withClaim("cargoNome", cargoNome(user))
                    .withClaim("permissoes", permissoesConcedidas(user))
                    .withClaim("sessionVersion", user.getSessaoVersao())
                    .withExpiresAt(generateExpirationDate())
                    .sign(algorithm);
            return token;
        }catch (JWTCreationException exception){
            throw new RuntimeException("Error while generating token",exception);
        }
    }

    private boolean isAdministrador(Usuario user) {
        return user.getCargo() != null && user.getCargo().isAdministrador();
    }

    /** String vazia em vez de null: o master não tem cargo relevante, e o claim é só rótulo de UI. */
    private String cargoNome(Usuario user) {
        return user.getCargo() != null ? user.getCargo().getNome() : "";
    }

    /**
     * Lista vazia para administrador de propósito: quem é admin tem acesso total (ver
     * Usuario.getAuthorities()) e o frontend trata `admin = true` como "tem tudo" — enumerar
     * todas as permissões no token só engordaria o JWT sem mudar decisão nenhuma.
     */
    private List<String> permissoesConcedidas(Usuario user) {
        if (user.getCargo() == null || user.getCargo().isAdministrador()) {
            return List.of();
        }
        return user.getCargo().getPermissoes().stream().map(Enum::name).toList();
    }

    /**
     * Retorna o token decodificado (id no subject, empresaId como claim) ou null se inválido/
     * expirado. Trocado de "retorna o subject como String" para expor os claims porque o
     * SecurityFilter agora precisa de dois valores (id + empresaId), não só um.
     */
    public DecodedJWT validateToken(String token){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("juridiqsystem")
                    .build()
                    .verify(token);
        }catch (JWTVerificationException exception){
            return null;
        }
    }

    private Instant generateExpirationDate(){
        return LocalDateTime.now().plusHours(12).toInstant(ZoneOffset.of("-03:00"));
    }
}

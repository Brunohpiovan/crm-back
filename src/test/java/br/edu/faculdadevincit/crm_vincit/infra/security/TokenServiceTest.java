package br.edu.faculdadevincit.crm_vincit.infra.security;

import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o shape real do claim "roles" do JWT gerado pelo backend (usado pelo AuthService.isAdmin()
 * do frontend) e confirma que um token opaco de recuperação de senha (não-JWT) não é aceito como
 * autenticação normal pelo SecurityFilter.
 */
class TokenServiceTest {

    private static final String SEGREDO_DE_TESTE = "segredo-de-teste-nao-usado-em-producao";

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", SEGREDO_DE_TESTE);
    }

    private Usuario usuarioComCargo(UserRole cargo) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin("usuario@teste.com");
        usuario.setNome("Usuário Teste");
        usuario.setCargo(cargo);
        return usuario;
    }

    @Test
    void generateToken_claimRolesEhAStringDoNomeDoEnumParaAdministrador() {
        String token = tokenService.generateToken(usuarioComCargo(UserRole.ADMINISTRADOR));

        DecodedJWT decoded = JWT.decode(token);

        assertThat(decoded.getClaim("roles").asString()).isEqualTo("ADMINISTRADOR");
        assertThat(decoded.getSubject()).isEqualTo("usuario@teste.com");
    }

    @Test
    void generateToken_claimRolesEhAStringDoNomeDoEnumParaVendedor() {
        String token = tokenService.generateToken(usuarioComCargo(UserRole.VENDEDOR));

        DecodedJWT decoded = JWT.decode(token);

        assertThat(decoded.getClaim("roles").asString()).isEqualTo("VENDEDOR");
    }

    @Test
    void validateToken_tokenValido_retornaOLoginDoSubject() {
        String token = tokenService.generateToken(usuarioComCargo(UserRole.VENDEDOR));

        String login = tokenService.validateToken(token);

        assertThat(login).isEqualTo("usuario@teste.com");
    }

    @Test
    void validateToken_tokenExpirado_retornaVazio() {
        String tokenExpirado = JWT.create()
                .withIssuer("crm_vincit")
                .withSubject("usuario@teste.com")
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256(SEGREDO_DE_TESTE));

        String login = tokenService.validateToken(tokenExpirado);

        assertThat(login).isEmpty();
    }

    @Test
    void validateToken_tokenOpacoDeRecuperacaoDeSenha_naoEhAceitoComoAutenticacaoNormal() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String tokenDeRecuperacaoDeSenha = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String login = tokenService.validateToken(tokenDeRecuperacaoDeSenha);

        assertThat(login).isEmpty();
    }
}

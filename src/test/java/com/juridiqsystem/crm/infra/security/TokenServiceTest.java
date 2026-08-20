package com.juridiqsystem.crm.infra.security;

import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.Cargo;
import com.juridiqsystem.crm.model.enums.Permissao;
import com.juridiqsystem.crm.testsupport.TestCargoFactory;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre o shape real dos claims de autorização do JWT gerado pelo backend ("admin", "master",
 * "cargoNome" e "permissoes", usados por lib/auth.ts no frontend e que substituíram o antigo claim
 * único "roles", que carregava o nome do enum UserRole), o subject como publicId (UUID) do usuário
 * — não o id numérico interno, que um JWT decodificado no navegador exporia — e o claim "empresaId"
 * (multi-tenant — ver SecurityFilter/TenantContext); e confirma que um token opaco de recuperação
 * de senha (não-JWT) não é aceito como autenticação normal pelo SecurityFilter.
 */
class TokenServiceTest {

    private static final String SEGREDO_DE_TESTE = "segredo-de-teste-nao-usado-em-producao";
    private static final String PUBLIC_ID_DE_TESTE = "550e8400-e29b-41d4-a716-446655440000";

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", SEGREDO_DE_TESTE);
    }

    private Usuario usuarioComCargo(Cargo cargo) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setPublicId(PUBLIC_ID_DE_TESTE);
        usuario.setEmpresaId(9L);
        usuario.setLogin("usuario@teste.com");
        usuario.setNome("Usuário Teste");
        usuario.setCargo(cargo);
        usuario.setMaster(false);
        return usuario;
    }

    @Test
    void generateToken_cargoAdministrador_marcaAdminSemEnumerarPermissoes() {
        String token = tokenService.generateToken(usuarioComCargo(TestCargoFactory.administrador()));

        DecodedJWT decoded = JWT.decode(token);

        assertThat(decoded.getClaim("admin").asBoolean()).isTrue();
        assertThat(decoded.getClaim("master").asBoolean()).isFalse();
        assertThat(decoded.getClaim("cargoNome").asString()).isEqualTo("Administrador");
        // Administrador tem acesso total pelo flag: enumerar as permissões no token só engordaria
        // o JWT — o frontend trata admin = true como "tem tudo" (ver hasPermissao em lib/auth.ts).
        assertThat(decoded.getClaim("permissoes").asList(String.class)).isEmpty();
        assertThat(decoded.getSubject()).isEqualTo(PUBLIC_ID_DE_TESTE);
        assertThat(decoded.getClaim("empresaId").asLong()).isEqualTo(9L);
    }

    @Test
    void generateToken_cargoCustomizado_listaSomenteAsPermissoesDelegadas() {
        Usuario usuario = usuarioComCargo(TestCargoFactory.comum("Advogado", Permissao.GERENCIAR_TEMPLATE_EMAIL));

        DecodedJWT decoded = JWT.decode(tokenService.generateToken(usuario));

        assertThat(decoded.getClaim("admin").asBoolean()).isFalse();
        assertThat(decoded.getClaim("cargoNome").asString()).isEqualTo("Advogado");
        assertThat(decoded.getClaim("permissoes").asList(String.class))
                .isEqualTo(List.of(Permissao.GERENCIAR_TEMPLATE_EMAIL.name()));
    }

    @Test
    void generateToken_usuarioMaster_marcaMasterEnaoAdmin() {
        Usuario usuario = usuarioComCargo(TestCargoFactory.administrador());
        usuario.setMaster(true);

        DecodedJWT decoded = JWT.decode(tokenService.generateToken(usuario));

        assertThat(decoded.getClaim("master").asBoolean()).isTrue();
    }

    @Test
    void validateToken_tokenValido_retornaOPublicIdDoSubjectEEmpresaIdComoClaim() {
        String token = tokenService.generateToken(usuarioComCargo(TestCargoFactory.comum()));

        DecodedJWT decoded = tokenService.validateToken(token);

        assertThat(decoded).isNotNull();
        assertThat(decoded.getSubject()).isEqualTo(PUBLIC_ID_DE_TESTE);
        assertThat(decoded.getClaim("empresaId").asLong()).isEqualTo(9L);
    }

    @Test
    void validateToken_tokenExpirado_retornaNulo() {
        String tokenExpirado = JWT.create()
                .withIssuer("juridiqsystem")
                .withSubject(PUBLIC_ID_DE_TESTE)
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256(SEGREDO_DE_TESTE));

        DecodedJWT decoded = tokenService.validateToken(tokenExpirado);

        assertThat(decoded).isNull();
    }

    @Test
    void validateToken_tokenOpacoDeRecuperacaoDeSenha_naoEhAceitoComoAutenticacaoNormal() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String tokenDeRecuperacaoDeSenha = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        DecodedJWT decoded = tokenService.validateToken(tokenDeRecuperacaoDeSenha);

        assertThat(decoded).isNull();
    }
}

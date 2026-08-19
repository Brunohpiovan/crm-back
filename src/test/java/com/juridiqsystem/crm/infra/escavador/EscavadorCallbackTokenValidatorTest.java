package com.juridiqsystem.crm.infra.escavador;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O webhook /webhooks/escavador/callback é público: sem esta validação, qualquer um poderia
 * inserir movimentações forjadas em processos de qualquer empresa. Estes testes fixam as duas
 * garantias que sustentam isso — token exato e comportamento fail-closed.
 */
class EscavadorCallbackTokenValidatorTest {

    private static final String TOKEN = "s3gr3do-do-painel-escavador";

    private final EscavadorCallbackTokenValidator validator = new EscavadorCallbackTokenValidator();

    @Test
    void isValid_comTokenNoHeaderAuthorization_aceita() {
        assertThat(validator.isValid(TOKEN, null, TOKEN)).isTrue();
    }

    @Test
    void isValid_comTokenNoHeaderPrefixadoPorBearer_aceita() {
        assertThat(validator.isValid("Bearer " + TOKEN, null, TOKEN)).isTrue();
    }

    @Test
    void isValid_comTokenNoQueryParam_aceita() {
        assertThat(validator.isValid(null, TOKEN, TOKEN)).isTrue();
    }

    @Test
    void isValid_comTokenDiferente_rejeita() {
        assertThat(validator.isValid("outro-token", null, TOKEN)).isFalse();
        assertThat(validator.isValid(null, "outro-token", TOKEN)).isFalse();
    }

    @Test
    void isValid_semTokenNenhum_rejeita() {
        assertThat(validator.isValid(null, null, TOKEN)).isFalse();
        assertThat(validator.isValid("", "  ", TOKEN)).isFalse();
    }

    /** Fail-closed: aplicação sem ESCAVADOR_CALLBACK_TOKEN configurado não aceita callback algum. */
    @Test
    void isValid_semTokenConfigurado_rejeitaAteMesmoUmaChamadaSemToken() {
        assertThat(validator.isValid(null, null, null)).isFalse();
        assertThat(validator.isValid("qualquer-coisa", "qualquer-coisa", "")).isFalse();
    }
}

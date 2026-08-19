package com.juridiqsystem.crm.infra.security;

import java.util.function.Supplier;

/**
 * Guarda o id da Empresa (tenant) da requisição atual, numa ThreadLocal. Populado por
 * SecurityFilter logo após resolver o Usuario autenticado, e SEMPRE limpo no finally do
 * mesmo filtro — as worker threads do Tomcat são reaproveitadas entre requisições, então
 * esquecer de limpar vazaria o tenant de uma requisição para a próxima que caísse na mesma
 * thread. Lido por TenantIdentifierResolver a cada acesso ao banco.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_EMPRESA_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long empresaId) {
        CURRENT_EMPRESA_ID.set(empresaId);
    }

    public static Long get() {
        return CURRENT_EMPRESA_ID.get();
    }

    public static void clear() {
        CURRENT_EMPRESA_ID.remove();
    }

    /**
     * Executa `action` com o tenant temporariamente trocado para `empresaId`, restaurando o
     * tenant anterior ao final (mesmo em caso de exceção). Usado pelos endpoints /master/**,
     * onde o usuário master (que pertence à empresa interna) precisa operar sobre os dados de
     * uma empresa-cliente específica — generaliza o bypass manual já usado em
     * AuthenticationService.login.
     */
    public static <T> T runAs(Long empresaId, Supplier<T> action) {
        Long previous = CURRENT_EMPRESA_ID.get();
        CURRENT_EMPRESA_ID.set(empresaId);
        try {
            return action.get();
        } finally {
            if (previous != null) {
                CURRENT_EMPRESA_ID.set(previous);
            } else {
                CURRENT_EMPRESA_ID.remove();
            }
        }
    }
}

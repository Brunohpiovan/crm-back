package br.edu.faculdadevincit.crm_vincit.infra.security;

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
}

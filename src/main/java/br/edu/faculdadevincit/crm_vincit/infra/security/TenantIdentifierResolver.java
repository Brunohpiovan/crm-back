package br.edu.faculdadevincit.crm_vincit.infra.security;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Resolve o tenant (empresa) atual para o Hibernate, usado pelas entidades anotadas com
 * @TenantId (ver TenantContext e HibernateConfig). Fail-safe por design: se o TenantContext
 * não tiver sido populado (bug no SecurityFilter, ou acesso sem Usuario autenticado a uma
 * entidade tenant-scoped), retorna um sentinel que não bate com nenhuma Empresa real — a
 * query tem que voltar vazia nesse caso, nunca sem filtro nenhum.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    private static final Long NO_TENANT = -1L;

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long empresaId = TenantContext.get();
        return empresaId != null ? empresaId : NO_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}

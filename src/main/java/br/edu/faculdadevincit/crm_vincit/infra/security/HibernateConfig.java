package br.edu.faculdadevincit.crm_vincit.infra.security;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra o TenantIdentifierResolver no Hibernate para que as entidades anotadas com
 * @TenantId sejam automaticamente filtradas/populadas pelo tenant (empresa) atual. Não
 * precisa de MultiTenantConnectionProvider/troca de schema — é um único DataSource, mesmo
 * HikariCP de sempre; @TenantId só adiciona um predicado de WHERE/valor de INSERT por baixo
 * dos panos, não muda como a conexão com o banco é obtida.
 */
@Configuration
public class HibernateConfig {

    @Autowired
    private TenantIdentifierResolver tenantIdentifierResolver;

    @Bean
    public HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer() {
        return hibernateProperties ->
                hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
    }
}

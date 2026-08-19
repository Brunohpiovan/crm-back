package com.juridiqsystem.crm.infra.escavador;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração do webhook público que recebe os callbacks da Escavador. Prefixo próprio
 * ("escavador.callback.*") para não colidir com as propriedades do cliente HTTP genérico
 * ("escavador.*") — nenhuma outra classe deve ler ESCAVADOR_CALLBACK_* via @Value.
 *
 * <p>Ver docs/integrations/escavador-callback.md para como gerar o token no painel da API.</p>
 */
@Component
@ConfigurationProperties(prefix = "escavador.callback")
public class EscavadorCallbackProperties {

    /**
     * Segredo compartilhado que autentica o callback. Gerado por nós no painel da API da
     * Escavador (https://api.escavador.com/callbacks), que passa a enviá-lo no header
     * Authorization de todo callback. Vazio desabilita o endpoint (fail-closed): sem segredo
     * configurado, nenhum callback é aceito — o contrário deixaria um endpoint público capaz de
     * inserir movimentações forjadas em processos de qualquer empresa.
     */
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

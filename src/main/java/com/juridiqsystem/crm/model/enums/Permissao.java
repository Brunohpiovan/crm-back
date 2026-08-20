package com.juridiqsystem.crm.model.enums;

/**
 * Catálogo fixo (do sistema, não editável pela empresa) das capacidades que um administrador
 * pode delegar a um Cargo customizado. Cada valor vira a authority "PERM_<NOME>" em
 * Usuario.getAuthorities(), consumida pelas regras de rota em SecurityConfiguration.
 *
 * <p>Só entra aqui o que é configuração de negócio compartilhada da empresa (funil, cadência,
 * template, tag). Gestão de pessoas/segurança/infraestrutura (CRUD de usuário, dados da
 * empresa, integração de WhatsApp, log de acesso, os próprios cargos) é hard-admin: continua
 * exigindo ROLE_ADMIN e deliberadamente NÃO tem Permissao correspondente — delegar isso
 * significaria delegar a própria capacidade de escalar privilégio.</p>
 */
public enum Permissao {

    GERENCIAR_FUNIL("Gerenciar funis e etapas"),
    GERENCIAR_CADENCIA("Gerenciar cadências de funil"),
    GERENCIAR_TEMPLATE_EMAIL("Gerenciar templates de e-mail"),
    GERENCIAR_TAG("Gerenciar tags");

    private final String rotulo;

    Permissao(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Texto exibido ao administrador na tela de cargos (fonte única de verdade do rótulo). */
    public String getRotulo() {
        return rotulo;
    }
}

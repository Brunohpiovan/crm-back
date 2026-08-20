package com.juridiqsystem.crm.testsupport;

import com.juridiqsystem.crm.model.Cargo;
import com.juridiqsystem.crm.model.enums.Permissao;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;

/**
 * Monta Cargo em memória para os testes que antes atribuíam o enum UserRole direto no Usuario.
 * Cobre os três casos que a autorização distingue hoje: o cargo administrador (acesso total pelo
 * flag, sem depender de cargo_permissao), um cargo comum sem nenhuma permissão delegada e um
 * cargo comum com permissões específicas.
 *
 * <p>Os objetos vêm com publicId preenchido para poderem ser usados também sem persistir (o
 * @UuidGenerator só age no insert).</p>
 */
public final class TestCargoFactory {

    private TestCargoFactory() {
    }

    /** Cargo administrador da empresa: equivalente ao antigo UserRole.ADMINISTRADOR. */
    public static Cargo administrador() {
        return cargo("Administrador", true);
    }

    /** Cargo comum sem permissão delegada nenhuma: equivalente ao antigo UserRole.VENDEDOR. */
    public static Cargo comum() {
        return cargo("Funcionário", false);
    }

    /** Cargo comum com permissões específicas (ex.: um "Advogado" que só gerencia templates). */
    public static Cargo comum(String nome, Permissao... permissoes) {
        Cargo cargo = cargo(nome, false);
        // LinkedHashSet sobre a lista (e não Set.of) para preservar a ordem informada: os testes
        // que conferem o claim "permissoes" do JWT comparam a lista posição a posição.
        cargo.setPermissoes(new LinkedHashSet<>(Arrays.asList(permissoes)));
        return cargo;
    }

    /**
     * Mesmo valor que o TenantIdentifierResolver atribui ao Usuario nos testes sem TenantContext
     * populado (sentinel NO_TENANT), para que cargo e usuário fiquem na "mesma empresa" quando o
     * teste persiste os dois. empresa_id é NOT NULL e, diferente das entidades @TenantId, não é
     * preenchido automaticamente pelo Hibernate (ver Cargo).
     */
    private static final Long EMPRESA_ID_DE_TESTE = -1L;

    private static Cargo cargo(String nome, boolean administrador) {
        Cargo cargo = new Cargo();
        cargo.setEmpresaId(EMPRESA_ID_DE_TESTE);
        cargo.setPublicId(UUID.randomUUID().toString());
        cargo.setNome(nome);
        cargo.setAdministrador(administrador);
        cargo.setPermissoes(new LinkedHashSet<>());
        return cargo;
    }
}

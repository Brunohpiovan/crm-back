package com.juridiqsystem.crm.model;

import com.juridiqsystem.crm.model.enums.Permissao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Cargo de uma empresa ("Advogado", "Secretário", "Estagiário"...): nome livre, definido pelo
 * próprio administrador do escritório, com um conjunto de Permissao delegadas.
 *
 * <p>Cada empresa tem exatamente um cargo com {@code administrador = true} (criado no backfill
 * / na criação da empresa): ele significa acesso total e não depende de nenhuma linha em
 * cargo_permissao — ver Usuario.getAuthorities(). Não pode ser excluído nem editado; só
 * renomeado não é permitido pela API (é fixo do sistema). Cargo é diferente de "master", que é
 * super-admin da plataforma (Usuario.master), não um cargo de empresa-cliente.</p>
 */
@Schema(description = "Cargo customizável por empresa, com o conjunto de permissões delegadas a quem o ocupa.")
@Getter
@Setter
@Entity
@Table(name = "cargo", uniqueConstraints = @UniqueConstraint(name = "uk_cargo_empresa_nome", columnNames = {"empresa_id", "nome"}))
@NoArgsConstructor
@AllArgsConstructor
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    /**
     * Empresa dona do cargo. Diferente das outras entidades do CRM, NÃO usa @TenantId: Usuario
     * referencia Cargo com fetch EAGER, e o Usuario é carregado em dois fluxos que rodam antes de
     * existir tenant resolvido — o login (AuthorizationService.loadUserByUsername, que descobre a
     * empresa pelo próprio login) e o reset de senha (findByIdIgnorandoTenant). Com @TenantId,
     * esse carregamento do cargo cairia no sentinel -1 do TenantIdentifierResolver e falharia
     * justamente ao autenticar.
     *
     * <p>O isolamento por empresa continua garantido, só que explicitamente: todo acesso a Cargo
     * passa por CargoRepository, cujos métodos exigem o empresaId; o único acesso sem filtro é
     * usuario.getCargo(), que por definição já é o cargo da empresa do próprio usuário.</p>
     */
    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @NotBlank(message = "Informe o nome do cargo")
    @Size(max = 100, message = "O nome do cargo deve ter no maximo 100 caracteres")
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Schema(description = "Se true, é o cargo de administrador da empresa: acesso total, imutável e não excluível.")
    @Column(name = "administrador", nullable = false)
    private boolean administrador;

    /**
     * EAGER de propósito: getAuthorities() é chamado fora de qualquer sessão JPA aberta
     * (open-in-view=false, e no WebSocket o Usuario fica guardado nos sessionAttributes entre
     * frames STOMP) — com LAZY isso estouraria LazyInitializationException em toda requisição
     * autenticada. São no máximo alguns valores por cargo, e a empresa tem poucos cargos.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "cargo_permissao",
            joinColumns = @JoinColumn(name = "cargo_id", foreignKey = @ForeignKey(name = "fk_cargo_permissao_cargo"))
    )
    @Column(name = "permissao", nullable = false, length = 60)
    @Enumerated(EnumType.STRING)
    private Set<Permissao> permissoes = new LinkedHashSet<>();

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}

package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Equipe;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.enums.Uf;
import com.juridiqsystem.crm.model.enums.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.Hibernate.isInitialized;

/**
 * Cobre o item de alta prioridade da auditoria (N+1 em EquipeRepository/EquipeService.findAll):
 * findAll() precisa trazer os membros já carregados via @EntityGraph, não sob demanda um a um
 * quando EquipeResponse acessa equipe.getMembros().
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:equipe_repo_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class EquipeRepositoryTest {

    @Autowired
    private EquipeRepository equipeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findAllTrazMembrosJaCarregadosSemLazyLoadingAdicional() {
        Usuario membro = usuarioRepository.save(criaUsuario("Ana", "ana@juridiqsystem.com.br"));

        Equipe equipe = new Equipe();
        equipe.setNome("Equipe Vendas");
        equipe.getMembros().add(membro);
        equipeRepository.save(equipe);

        entityManager.flush();
        entityManager.clear();

        List<Equipe> equipes = equipeRepository.findAll();

        assertThat(equipes).hasSize(1);
        // @EntityGraph garante que "membros" veio no mesmo carregamento: um acesso após
        // entityManager.clear() (sem sessão para lazy-load) só funciona se já estava inicializado.
        assertThat(isInitialized(equipes.get(0).getMembros())).isTrue();
        assertThat(equipes.get(0).getMembros())
                .extracting(Usuario::getNome)
                .containsExactly("Ana");
    }

    private Usuario criaUsuario(String nome, String login) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setLogin(login);
        usuario.setSenha("senha12345");
        usuario.setRg("1234567890");
        usuario.setCpf(String.valueOf(Math.abs(login.hashCode())));
        usuario.setDataNascimento(LocalDate.of(1995, 1, 1));
        usuario.setCelular("11999999999");
        usuario.setCargo(UserRole.VENDEDOR);
        usuario.setEndereco("Rua Teste");
        usuario.setNumeroResidencial("100");
        usuario.setBairro("Centro");
        usuario.setUf(Uf.SP);
        usuario.setCidade("Sao Paulo");
        usuario.setCep("01000-000");
        usuario.setBloqueado(false);
        return usuario;
    }
}

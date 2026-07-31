package br.edu.faculdadevincit.crm_vincit.repository;

import br.edu.faculdadevincit.crm_vincit.model.Funil;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioContatoDto;
import br.edu.faculdadevincit.crm_vincit.model.enums.Uf;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UsuarioRepository.findDisponiveisParaFunil substitui o antigo findByNotCargo, que carregava
 * TODOS os usuários (entidade completa, com senha) e filtrava em memória em FunilService quem já
 * estava no funil. Este teste garante que a exclusão feita agora via SQL (NOT IN) produz o mesmo
 * resultado que a filtragem em memória anterior.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:usuario_repo_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private FunilRepository funilRepository;

    @Test
    void findDisponiveisParaFunilExcluiFuncionariosDoFunilEAdministradores() {
        Usuario jaNoFunil = usuarioRepository.save(criaUsuario("Ana", "ana@vincit.edu.br", UserRole.VENDEDOR));
        Usuario disponivel = usuarioRepository.save(criaUsuario("Bruno", "bruno@vincit.edu.br", UserRole.VENDEDOR));
        usuarioRepository.save(criaUsuario("Carla", "carla@vincit.edu.br", UserRole.ADMINISTRADOR));

        Funil funil = new Funil();
        funil.setNome("Funil Vendas");
        List<Usuario> funcionarios = new ArrayList<>();
        funcionarios.add(jaNoFunil);
        funil.setFuncionarios(funcionarios);
        Long funilId = funilRepository.save(funil).getId();

        List<UsuarioContatoDto> disponiveis =
                usuarioRepository.findDisponiveisParaFunil(funilId, UserRole.ADMINISTRADOR);

        assertThat(disponiveis)
                .extracting(UsuarioContatoDto::getId)
                .containsExactly(disponivel.getId());
    }

    @Test
    void findDisponiveisParaFunilRetornaTodosNaoAdministradoresQuandoFunilVazio() {
        Usuario vendedor1 = usuarioRepository.save(criaUsuario("Diego", "diego@vincit.edu.br", UserRole.VENDEDOR));
        Usuario vendedor2 = usuarioRepository.save(criaUsuario("Elis", "elis@vincit.edu.br", UserRole.VENDEDOR));
        usuarioRepository.save(criaUsuario("Fabio", "fabio@vincit.edu.br", UserRole.ADMINISTRADOR));

        Funil funil = new Funil();
        funil.setNome("Funil Vazio");
        funil.setFuncionarios(new ArrayList<>());
        Long funilId = funilRepository.save(funil).getId();

        List<UsuarioContatoDto> disponiveis =
                usuarioRepository.findDisponiveisParaFunil(funilId, UserRole.ADMINISTRADOR);

        assertThat(disponiveis)
                .extracting(UsuarioContatoDto::getId)
                .containsExactlyInAnyOrder(vendedor1.getId(), vendedor2.getId());
    }

    private Usuario criaUsuario(String nome, String login, UserRole cargo) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setLogin(login);
        usuario.setSenha("senha12345");
        usuario.setRg("1234567890");
        usuario.setCpf(String.valueOf(Math.abs(login.hashCode())));
        usuario.setDataNascimento(LocalDate.of(1995, 1, 1));
        usuario.setCelular("11999999999");
        usuario.setCargo(cargo);
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

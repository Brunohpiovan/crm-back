package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Cargo;
import com.juridiqsystem.crm.model.Funil;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.UsuarioContatoDto;
import com.juridiqsystem.crm.model.enums.Uf;
import com.juridiqsystem.crm.testsupport.TestCargoFactory;
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
 *
 * <p>"Administrador" deixou de ser um valor de enum e passou a ser o flag do Cargo da empresa
 * (cargos são customizáveis por escritório), então a exclusão dos administradores é testada com
 * dois cargos reais persistidos, não mais com UserRole.</p>
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

    @Autowired
    private CargoRepository cargoRepository;

    @Test
    void findDisponiveisParaFunilExcluiFuncionariosDoFunilEAdministradores() {
        Cargo comum = cargoRepository.save(TestCargoFactory.comum());
        Cargo administrador = cargoRepository.save(TestCargoFactory.administrador());
        Usuario jaNoFunil = usuarioRepository.save(criaUsuario("Ana", "ana@juridiqsystem.com.br", comum));
        Usuario disponivel = usuarioRepository.save(criaUsuario("Bruno", "bruno@juridiqsystem.com.br", comum));
        usuarioRepository.save(criaUsuario("Carla", "carla@juridiqsystem.com.br", administrador));

        Funil funil = new Funil();
        funil.setNome("Funil Vendas");
        List<Usuario> funcionarios = new ArrayList<>();
        funcionarios.add(jaNoFunil);
        funil.setFuncionarios(funcionarios);
        Long funilId = funilRepository.save(funil).getId();

        List<UsuarioContatoDto> disponiveis =
                usuarioRepository.findDisponiveisParaFunil(funilId);

        assertThat(disponiveis)
                .extracting(UsuarioContatoDto::getId)
                .containsExactly(disponivel.getPublicId());
    }

    @Test
    void findDisponiveisParaFunilRetornaTodosNaoAdministradoresQuandoFunilVazio() {
        Cargo comum = cargoRepository.save(TestCargoFactory.comum());
        Cargo administrador = cargoRepository.save(TestCargoFactory.administrador());
        Usuario vendedor1 = usuarioRepository.save(criaUsuario("Diego", "diego@juridiqsystem.com.br", comum));
        Usuario vendedor2 = usuarioRepository.save(criaUsuario("Elis", "elis@juridiqsystem.com.br", comum));
        usuarioRepository.save(criaUsuario("Fabio", "fabio@juridiqsystem.com.br", administrador));

        Funil funil = new Funil();
        funil.setNome("Funil Vazio");
        funil.setFuncionarios(new ArrayList<>());
        Long funilId = funilRepository.save(funil).getId();

        List<UsuarioContatoDto> disponiveis =
                usuarioRepository.findDisponiveisParaFunil(funilId);

        assertThat(disponiveis)
                .extracting(UsuarioContatoDto::getId)
                .containsExactlyInAnyOrder(vendedor1.getPublicId(), vendedor2.getPublicId());
    }

    private Usuario criaUsuario(String nome, String login, Cargo cargo) {
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

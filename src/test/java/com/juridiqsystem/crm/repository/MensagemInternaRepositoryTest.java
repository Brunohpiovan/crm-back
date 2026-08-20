package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Cargo;
import com.juridiqsystem.crm.model.ChatGrupo;
import com.juridiqsystem.crm.model.MensagemInterna;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.enums.Uf;
import com.juridiqsystem.crm.testsupport.TestCargoFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MensagemInternaRepository.findByChatGrupo deixou de ser uma query derivada (que causava N+1
 * duplo em chatGrupo/sender, ambos EAGER por padrão) e passou a usar JOIN FETCH no sender,
 * mantendo a paginação via countQuery dedicada. Este teste garante que a paginação e o filtro por
 * grupo continuam corretos.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:mensagem_interna_repo_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MensagemInternaRepositoryTest {

    @Autowired
    private MensagemInternaRepository mensagemInternaRepository;

    @Autowired
    private ChatGrupoRepository chatGrupoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CargoRepository cargoRepository;

    private Cargo cargoComum;

    @Test
    void findByChatGrupoRetornaApenasMensagensDoGrupoComSenderCarregado() {
        Usuario sender = usuarioRepository.save(criaUsuario("Diego", "diego@juridiqsystem.com.br"));

        ChatGrupo grupoAlvo = chatGrupoRepository.save(criaGrupo("Grupo Alvo", sender));
        ChatGrupo outroGrupo = chatGrupoRepository.save(criaGrupo("Outro Grupo", sender));

        mensagemInternaRepository.save(criaMensagem(grupoAlvo, sender, "Mensagem 1", LocalDateTime.now().minusMinutes(2)));
        mensagemInternaRepository.save(criaMensagem(grupoAlvo, sender, "Mensagem 2", LocalDateTime.now().minusMinutes(1)));
        mensagemInternaRepository.save(criaMensagem(outroGrupo, sender, "Mensagem de outro grupo", LocalDateTime.now()));

        Page<MensagemInterna> pagina = mensagemInternaRepository.findByChatGrupo(
                grupoAlvo, PageRequest.of(0, 10, Sort.by("id").descending()));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).hasSize(2);
        assertThat(pagina.getContent().get(0).getSender().getNome()).isEqualTo("Diego");
        assertThat(pagina.getContent())
                .allSatisfy(m -> assertThat(m.getChatGrupo().getId()).isEqualTo(grupoAlvo.getId()));
    }

    private ChatGrupo criaGrupo(String nome, Usuario usuario) {
        ChatGrupo grupo = new ChatGrupo();
        grupo.setNome(nome);
        grupo.setPrivado(false);
        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(usuario);
        grupo.setUsuarios(usuarios);
        return grupo;
    }

    private MensagemInterna criaMensagem(ChatGrupo grupo, Usuario sender, String conteudo, LocalDateTime dataEnvio) {
        MensagemInterna mensagem = new MensagemInterna();
        mensagem.setChatGrupo(grupo);
        mensagem.setSender(sender);
        mensagem.setConteudo(conteudo);
        mensagem.setDataEnvio(dataEnvio);
        return mensagem;
    }

    private Usuario criaUsuario(String nome, String login) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setLogin(login);
        usuario.setSenha("senha12345");
        usuario.setRg("654321");
        usuario.setCpf(String.valueOf(Math.abs(login.hashCode())));
        usuario.setDataNascimento(LocalDate.of(1990, 5, 10));
        usuario.setCelular("11988888888");
        usuario.setCargo(cargoComum());
        usuario.setEndereco("Rua Exemplo");
        usuario.setNumeroResidencial("50");
        usuario.setBairro("Bairro");
        usuario.setUf(Uf.SP);
        usuario.setCidade("Sao Paulo");
        usuario.setCep("02000-000");
        usuario.setBloqueado(false);
        return usuario;
    }

    /**
     * Cargo persistido uma vez por teste: usuario.cargo_id é FK NOT NULL desde que o cargo
     * passou a ser entidade por empresa (antes era o enum UserRole gravado na própria linha).
     */
    private Cargo cargoComum() {
        if (cargoComum == null) {
            cargoComum = cargoRepository.save(TestCargoFactory.comum());
        }
        return cargoComum;
    }
}

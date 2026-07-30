package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.infra.security.TokenService;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.UsuarioSelfUpdateDTO;
import br.edu.faculdadevincit.crm_vincit.model.enums.Uf;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Cobre a correção de segurança do PUT /usuario/{id}: o payload de autoatualização
 * (UsuarioSelfUpdateDTO) não possui os campos cargo/bloqueado, então mesmo que um
 * usuário comum tente forjar esses valores eles nunca chegam ao service. Também cobre
 * o delete(), que passa a inativar o usuário em vez de removê-lo (ver UsuarioService.delete).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceUpdateTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BCryptPasswordEncoder encoder;

    @Mock
    private S3Service s3Service;

    @Mock
    private TokenService tokenService;

    @Mock
    private ParticipanteService participanteService;

    @Mock
    private CloudFrontService cloudFrontService;

    @InjectMocks
    private UsuarioService usuarioService;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void stubsComuns() {
        lenient().when(cloudFrontService.getBaseUrl()).thenReturn("https://cdn-nao-usado-neste-teste.example.com/");
    }

    private Usuario usuarioExistente() {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        usuario.setLogin("alvo@teste.com");
        usuario.setNome("Usuário Alvo");
        usuario.setUrlPicture("assets/img/avatar/padrao.jpeg");
        usuario.setCargo(UserRole.VENDEDOR);
        usuario.setBloqueado(false);
        usuario.setRg("123456");
        usuario.setCpf("11122233344");
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setCelular("11999999999");
        usuario.setEndereco("Rua Teste");
        usuario.setNumeroResidencial("100");
        usuario.setBairro("Centro");
        usuario.setUf(Uf.SP);
        usuario.setCidade("São Paulo");
        usuario.setCep("01000-000");
        return usuario;
    }

    private UsuarioSelfUpdateDTO selfUpdateDto() {
        UsuarioSelfUpdateDTO dto = new UsuarioSelfUpdateDTO();
        dto.setUrlPicture("assets/img/avatar/padrao.jpeg");
        dto.setNome("Usuário Alvo Editado");
        dto.setLogin("alvo@teste.com");
        dto.setRg("123456");
        dto.setCpf("11122233344");
        dto.setDataNascimento(LocalDate.of(1990, 1, 1));
        dto.setCelular("11999999999");
        dto.setEndereco("Rua Teste");
        dto.setNumeroResidencial("100");
        dto.setBairro("Centro");
        dto.setUf(Uf.SP);
        dto.setCidade("São Paulo");
        dto.setCep("01000-000");
        return dto;
    }

    private void autenticarComo(String login) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(login, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void autoatualizacao_naoAlteraCargoNemBloqueado() {
        Usuario existente = usuarioExistente();
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(existente));
        when(participanteService.findByLoginSystem(anyString())).thenReturn(new Participante());
        when(tokenService.generateToken(any(Usuario.class))).thenReturn("token-fake");
        autenticarComo("alvo@teste.com");

        usuarioService.update(42L, selfUpdateDto(), null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());

        assertThat(captor.getValue().getCargo()).isEqualTo(UserRole.VENDEDOR);
        assertThat(captor.getValue().getBloqueado()).isFalse();
        assertThat(captor.getValue().getNome()).isEqualTo("Usuário Alvo Editado");
    }

    @Test
    void exclusao_inativaUsuarioEmVezDeApagarFisicamente() {
        Usuario existente = usuarioExistente();
        when(usuarioRepository.findById(42L)).thenReturn(Optional.of(existente));

        usuarioService.delete(42L);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        verify(usuarioRepository, never()).delete(any(Usuario.class));
        verify(usuarioRepository, never()).deleteById(anyLong());

        assertThat(captor.getValue().getBloqueado()).isTrue();
    }
}

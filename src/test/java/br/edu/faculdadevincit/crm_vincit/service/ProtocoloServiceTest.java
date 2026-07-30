package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import br.edu.faculdadevincit.crm_vincit.repository.MensagemRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ParticipanteRepository;
import br.edu.faculdadevincit.crm_vincit.repository.ProtocoloRepository;
import br.edu.faculdadevincit.crm_vincit.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtocoloServiceTest {

    @Mock
    private ProtocoloRepository protocoloRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ParticipanteRepository participanteRepository;

    @Mock
    private MensagemRepository mensagemRepository;

    @InjectMocks
    private ProtocoloService protocoloService;

    @BeforeEach
    void autenticarComoAdmin() {
        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setLogin("admin@teste.com");
        admin.setCargo(UserRole.ADMINISTRADOR);

        lenient().when(usuarioRepository.findByLogin("admin@teste.com"))
                .thenReturn(Optional.of((UserDetails) admin));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@teste.com", null, admin.getAuthorities()));
    }

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    private Protocolo protocolo(StatusProtocolo status) {
        Participante participante = new Participante();
        participante.setId(2L);
        participante.setNome("Cliente Teste");

        Protocolo protocolo = new Protocolo();
        protocolo.setId(10L);
        protocolo.setParticipante(participante);
        protocolo.setStatus(status);
        return protocolo;
    }

    @Test
    void fecharProtocoloAberto_alteraStatusParaFechadoEPersiste() {
        Protocolo protocolo = protocolo(StatusProtocolo.ABERTO);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        protocoloService.closeProtocolo(10L);

        assertThat(protocolo.getStatus()).isEqualTo(StatusProtocolo.FECHADO);
        verify(protocoloRepository, times(1)).save(protocolo);
        // closeProtocolo notifica duas vezes: status do protocolo ("/topic/protocolo/{id}") e
        // liberação do contato ("/topic/contatoRet").
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(Object.class));
    }

    /**
     * Documenta um bug conhecido (auditoria, item 6): closeProtocolo compara
     * "FECHADO".equals(protocolo.getStatus()) — String contra enum, nunca true — então fechar um
     * protocolo já fechado hoje NÃO lança exceção. Este teste trava o comportamento atual; deve ser
     * atualizado quando esse bug for corrigido em etapa de correção dedicada (fora do escopo desta
     * etapa, que é só criar a rede de testes).
     */
    @Test
    void fecharProtocoloJaFechado_comportamentoAtualNaoLancaExcecao() {
        Protocolo protocolo = protocolo(StatusProtocolo.FECHADO);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        protocoloService.closeProtocolo(10L);

        assertThat(protocolo.getStatus()).isEqualTo(StatusProtocolo.FECHADO);
        verify(protocoloRepository, times(1)).save(protocolo);
    }
}

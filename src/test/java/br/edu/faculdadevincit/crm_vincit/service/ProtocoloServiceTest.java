package br.edu.faculdadevincit.crm_vincit.service;

import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.ProtocoloDto;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
     * Bug da auditoria (item 6) corrigido: closeProtocolo agora compara
     * StatusProtocolo.FECHADO.equals(protocolo.getStatus()) — enum contra enum — então fechar um
     * protocolo já fechado lança exceção e não reabre/reenvia notificação.
     */
    @Test
    void fecharProtocoloJaFechado_lancaExcecaoENaoPersisteNemNotifica() {
        Protocolo protocolo = protocolo(StatusProtocolo.FECHADO);
        when(protocoloRepository.findById(10L)).thenReturn(Optional.of(protocolo));

        assertThatThrownBy(() -> protocoloService.closeProtocolo(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Protocolo ja encerrado");

        assertThat(protocolo.getStatus()).isEqualTo(StatusProtocolo.FECHADO);
        verify(protocoloRepository, times(0)).save(protocolo);
        verify(messagingTemplate, times(0)).convertAndSend(anyString(), any(Object.class));
    }

    /**
     * Cobre o item crítico C5 da auditoria: a checagem findByParticipanteIdAndStatusAberto antes
     * do insert é um check-then-act (não garante nada sob concorrência sozinha). Quem garante a
     * invariante de fato é o índice único uk_protocolo_participante_aberto (migration
     * V2026.08.05.20.30.00): se duas requisições passarem pela checagem e uma perder a corrida no
     * insert, o save() deve estourar DataIntegrityViolationException, que createProtocolo precisa
     * traduzir para o mesmo erro de negócio do caminho feliz de rejeição.
     */
    @Test
    void createProtocolo_corridaConcorrenteNoInsert_lancaErroDeNegocioEmVezDeVazarExcecaoDeBanco() {
        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setLogin("admin@teste.com");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(protocoloRepository.findByParticipanteIdAndStatusAberto(2L, StatusProtocolo.ABERTO))
                .thenReturn(Optional.empty());

        Participante participante = new Participante();
        participante.setId(2L);

        when(protocoloRepository.save(any(Protocolo.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key: uk_protocolo_participante_aberto"));

        when(participanteRepository.findById(2L)).thenReturn(Optional.of(participante));

        ProtocoloDto dto = new ProtocoloDto();
        dto.setId_admin(1L);
        dto.setId_participante(2L);

        assertThatThrownBy(() -> protocoloService.createProtocolo(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Já existe um protocolo em andamento com esse usuário");

        verify(messagingTemplate, times(0)).convertAndSend(anyString(), any(Object.class));
    }
}

package br.edu.faculdadevincit.crm_vincit.controller;

import br.edu.faculdadevincit.crm_vincit.model.Mensagem;
import br.edu.faculdadevincit.crm_vincit.model.Participante;
import br.edu.faculdadevincit.crm_vincit.model.Protocolo;
import br.edu.faculdadevincit.crm_vincit.model.Usuario;
import br.edu.faculdadevincit.crm_vincit.model.dtos.MensagemResponseDTO;
import br.edu.faculdadevincit.crm_vincit.model.enums.StatusProtocolo;
import br.edu.faculdadevincit.crm_vincit.model.enums.UserRole;
import br.edu.faculdadevincit.crm_vincit.service.MensagemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rede de regressão: GET /messages/{protocoloId} usa Mensagem.protocolo.admin (Usuario, EAGER) para
 * montar a resposta. Este teste garante que nenhuma senha escape para o JSON devolvido ao chat de
 * atendimento, mesmo com o admin do protocolo carregando uma senha real.
 */
@ExtendWith(MockitoExtension.class)
class MensagemControllerTest {

    @Mock
    private MensagemService mensagemService;

    @InjectMocks
    private MensagemController mensagemController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mensagemController).build();
    }

    @Test
    void getMessages_naoDeveRetornarSenhaDoAdminDoProtocolo() throws Exception {
        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setNome("Admin Teste");
        admin.setLogin("admin@teste.com");
        admin.setSenha("$2a$10$hashDeSenhaSuperSecreta");
        admin.setCargo(UserRole.ADMINISTRADOR);

        Participante participanteAdmin = new Participante();
        participanteAdmin.setId(1L);
        participanteAdmin.setNome("Admin Teste");

        Protocolo protocolo = new Protocolo();
        protocolo.setId(10L);
        protocolo.setAdmin(admin);
        protocolo.setParticipante(participanteAdmin);
        protocolo.setStatus(StatusProtocolo.ABERTO);

        Participante sender = new Participante();
        sender.setId(2L);
        sender.setNome("Cliente Teste");
        sender.setUrlPicture("avatar.png");

        Mensagem mensagem = new Mensagem();
        mensagem.setId(100L);
        mensagem.setProtocolo(protocolo);
        mensagem.setSender(sender);
        mensagem.setConteudo("Olá, preciso de ajuda");
        mensagem.setData_envio(LocalDateTime.now());

        MensagemResponseDTO dto = new MensagemResponseDTO(mensagem);
        when(mensagemService.getMessagesForProtocol(eq(10L))).thenReturn(List.of(dto));

        mockMvc.perform(get("/messages/10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsStringIgnoringCase("senha"))))
                .andExpect(jsonPath("$[0].conteudo").value("Olá, preciso de ajuda"))
                .andExpect(jsonPath("$[0].sender.senha").doesNotExist())
                .andExpect(jsonPath("$[0].protocolo.admin").doesNotExist());
    }

    /**
     * Item de alta prioridade da auditoria: limit sem teto máximo permitia pedir qualquer
     * quantidade de mensagens por página. Agora é clampado em 100 (mesmo padrão do Dashboard).
     */
    @Test
    void getMessagesLimit_limiteAcimaDoMaximo_ehClampadoAntesDeChamarOServico() throws Exception {
        when(mensagemService.getMessagesForProtocolLimit(eq(10L), eq(0), anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/messages/10").param("limit", "1000000").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(mensagemService).getMessagesForProtocolLimit(10L, 0, 100);
    }
}

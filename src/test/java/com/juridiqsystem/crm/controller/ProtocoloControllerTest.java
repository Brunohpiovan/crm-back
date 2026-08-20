package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.Participante;
import com.juridiqsystem.crm.model.Protocolo;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.ProtocoloMoveDTO;
import com.juridiqsystem.crm.model.enums.StatusProtocolo;
import com.juridiqsystem.crm.testsupport.TestCargoFactory;
import com.juridiqsystem.crm.service.ProtocoloService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rede de regressão para o vazamento de senha descrito na auditoria: garante que os endpoints de
 * Protocolo nunca serializam a senha do usuário administrador, mesmo quando a entidade de origem
 * (usada para montar o DTO) carrega uma senha real preenchida.
 */
@ExtendWith(MockitoExtension.class)
class ProtocoloControllerTest {

    @Mock
    private ProtocoloService protocoloService;

    @InjectMocks
    private ProtocoloController protocoloController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(protocoloController).build();
    }

    private Protocolo protocoloComSenhaRealNoAdmin() {
        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setPublicId("1");
        admin.setNome("Admin Teste");
        admin.setLogin("admin@teste.com");
        admin.setSenha("$2a$10$hashDeSenhaSuperSecreta");
        admin.setCargo(TestCargoFactory.administrador());

        Participante participante = new Participante();
        participante.setId(2L);
        participante.setNome("Cliente Teste");
        participante.setCelular("11999999999");

        Protocolo protocolo = new Protocolo();
        protocolo.setId(10L);
        protocolo.setAdmin(admin);
        protocolo.setParticipante(participante);
        protocolo.setStatus(StatusProtocolo.ABERTO);
        protocolo.setDataCriacao(LocalDateTime.now());
        return protocolo;
    }

    @Test
    void createProtocol_naoDeveRetornarSenhaDoAdmin() throws Exception {
        ProtocoloMoveDTO dto = new ProtocoloMoveDTO(protocoloComSenhaRealNoAdmin());
        when(protocoloService.createProtocolo(any())).thenReturn(dto);

        String body = "{\"id_admin\":\"1\",\"id_participante\":\"2\",\"status\":\"ABERTO\"}";

        mockMvc.perform(post("/protocolos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsStringIgnoringCase("senha"))))
                .andExpect(jsonPath("$.admin.id").value("1"))
                .andExpect(jsonPath("$.admin.senha").doesNotExist())
                .andExpect(jsonPath("$.admin.password").doesNotExist());
    }

    @Test
    void getProtocolo_listaNaoDeveRetornarSenhaDoAdmin() throws Exception {
        ProtocoloMoveDTO dto = new ProtocoloMoveDTO(protocoloComSenhaRealNoAdmin());
        when(protocoloService.getProtocols(eq("5"))).thenReturn(List.of(dto));

        mockMvc.perform(get("/protocolos/get/5").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsStringIgnoringCase("senha"))))
                .andExpect(jsonPath("$[0].admin.senha").doesNotExist())
                .andExpect(jsonPath("$[0].admin.password").doesNotExist());
    }

    @Test
    void findById_naoDeveRetornarSenhaDoAdmin() throws Exception {
        ProtocoloMoveDTO dto = new ProtocoloMoveDTO(protocoloComSenhaRealNoAdmin());
        when(protocoloService.findById(eq("10"))).thenReturn(dto);

        mockMvc.perform(get("/protocolos/10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsStringIgnoringCase("senha"))))
                .andExpect(jsonPath("$.admin.senha").doesNotExist())
                .andExpect(jsonPath("$.admin.password").doesNotExist());
    }
}

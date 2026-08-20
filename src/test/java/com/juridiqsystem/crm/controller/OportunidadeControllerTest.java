package com.juridiqsystem.crm.controller;

import com.juridiqsystem.crm.model.Etapa;
import com.juridiqsystem.crm.model.Oportunidade;
import com.juridiqsystem.crm.model.Participante;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.dtos.OportunidadeDTO;
import com.juridiqsystem.crm.model.enums.SituacaoOportunidade;
import com.juridiqsystem.crm.testsupport.TestCargoFactory;
import com.juridiqsystem.crm.service.OportunidadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rede de regressão: GET /oportunidade/cliente/{id} monta a resposta a partir de
 * Oportunidade.criador (Usuario, EAGER). Este teste garante que a senha do criador nunca
 * é serializada no JSON devolvido para a tela de chat de atendimento.
 */
@ExtendWith(MockitoExtension.class)
class OportunidadeControllerTest {

    @Mock
    private OportunidadeService oportunidadeService;

    @InjectMocks
    private OportunidadeController oportunidadeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(oportunidadeController).build();
    }

    @Test
    void findByClienteAndCriadorNull_naoDeveRetornarSenhaDoCriador() throws Exception {
        Usuario criador = new Usuario();
        criador.setId(1L);
        criador.setPublicId("1");
        criador.setNome("Vendedor Teste");
        criador.setLogin("vendedor@teste.com");
        criador.setSenha("$2a$10$hashDeSenhaSuperSecreta");
        criador.setCargo(TestCargoFactory.comum());

        Participante cliente = new Participante();
        cliente.setId(2L);
        cliente.setNome("Cliente Teste");
        cliente.setCelular("11999999999");

        Etapa etapa = new Etapa();
        etapa.setId(3L);

        Oportunidade oportunidade = new Oportunidade();
        oportunidade.setId(20L);
        oportunidade.setTitulo("Negociação Teste");
        oportunidade.setEtapa(etapa);
        oportunidade.setCriador(criador);
        oportunidade.setCliente(cliente);
        oportunidade.setValor(BigDecimal.TEN);
        oportunidade.setSituacao(SituacaoOportunidade.ABERTO);
        oportunidade.setIndice(0);

        OportunidadeDTO dto = new OportunidadeDTO(oportunidade);
        when(oportunidadeService.findByClienteAndCriadorNull(eq("2"))).thenReturn(dto);

        mockMvc.perform(get("/oportunidade/cliente/2").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsStringIgnoringCase("senha"))))
                .andExpect(jsonPath("$.criador.id").value("1"))
                .andExpect(jsonPath("$.criador.senha").doesNotExist())
                .andExpect(jsonPath("$.criador.password").doesNotExist());
    }
}

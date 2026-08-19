package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackMapper;
import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackProperties;
import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackTokenValidator;
import com.juridiqsystem.crm.model.EscavadorCallbackEvento;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.enums.FonteMovimentacao;
import com.juridiqsystem.crm.model.dtos.escavador.MovimentacaoInput;
import com.juridiqsystem.crm.repository.EscavadorCallbackEventoRepository;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.service.ProcessoMovimentacaoService;
import com.juridiqsystem.crm.service.exceptions.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O endpoint de callback é público: as garantias testadas aqui são a fronteira de segurança e de
 * auditoria do módulo — nenhum caminho pode terminar sem registro, e nenhum payload não
 * autenticado pode chegar ao banco.
 *
 * <p>LENIENT porque o stub de save() vale para quase todos os testes, menos justamente o de token
 * inválido — cujo ponto é que nada é gravado.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EscavadorCallbackServiceTest {

    private static final String TOKEN = "s3gr3do";
    private static final Long PROCESSO_ID = 10L;
    private static final String NUMERO_CNJ = "1002089-72.2023.8.26.0260";
    private static final String PAYLOAD = """
            {
                "event": "nova_movimentacao",
                "monitoramento": { "id": 1567024, "numero": "%s", "frequencia": "DIARIA", "status": "ENCONTRADO" },
                "movimentacao": { "id": 23895909833, "data": "2024-10-01", "tipo": "ANDAMENTO", "conteudo": "Pedido de Liminar" },
                "uuid": "65b45990e91de83f8f40483102ce97ca"
            }
            """.formatted(NUMERO_CNJ);

    @Spy
    private EscavadorCallbackTokenValidator tokenValidator = new EscavadorCallbackTokenValidator();

    @Spy
    private EscavadorCallbackProperties callbackProperties = new EscavadorCallbackProperties();

    @Spy
    private EscavadorCallbackMapper callbackMapper = new EscavadorCallbackMapper();

    @Mock
    private EscavadorCallbackEventoRepository escavadorCallbackEventoRepository;

    @Mock
    private ProcessoRepository processoRepository;

    @Mock
    private ProcessoMovimentacaoService processoMovimentacaoService;

    @Captor
    private ArgumentCaptor<List<MovimentacaoInput>> movimentacoesCaptor;

    @InjectMocks
    private EscavadorCallbackService escavadorCallbackService;

    @BeforeEach
    void configurar() {
        callbackProperties.setToken(TOKEN);
        when(escavadorCallbackEventoRepository.save(any(EscavadorCallbackEvento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void receber_comTokenInvalido_rejeitaENaoGravaNada() {
        assertThatThrownBy(() -> escavadorCallbackService.receber(PAYLOAD, "token-errado", null))
                .isInstanceOf(AccessDeniedException.class);

        verify(escavadorCallbackEventoRepository, never()).save(any());
        verify(processoMovimentacaoService, never()).registrarMovimentacoes(anyLong(), any());
    }

    @Test
    void receber_comCallbackValido_registraMovimentacaoEMarcaEventoComoProcessado() {
        prepararProcesso();

        escavadorCallbackService.receber(PAYLOAD, "Bearer " + TOKEN, null);

        verify(processoMovimentacaoService).registrarMovimentacoes(eq(PROCESSO_ID), movimentacoesCaptor.capture());
        List<MovimentacaoInput> inputs = movimentacoesCaptor.getValue();
        assertThat(inputs).hasSize(1);
        assertThat(inputs.get(0).fonte()).isEqualTo(FonteMovimentacao.CALLBACK_MONITORAMENTO);
        assertThat(inputs.get(0).conteudo()).isEqualTo("Pedido de Liminar");

        EscavadorCallbackEvento evento = ultimoEventoGravado();
        assertThat(evento.getProcessado()).isTrue();
        assertThat(evento.getNumeroCnjResolvido()).isEqualTo(NUMERO_CNJ);
        assertThat(evento.getErro()).isNull();
    }

    /**
     * O reenvio automático da Escavador (até 11 tentativas) faz o mesmo payload chegar repetido.
     * A deduplicação é responsabilidade de registrarMovimentacoes; o que se garante aqui é que o
     * service delega sempre a ele e nunca abre um caminho de escrita próprio.
     */
    @Test
    void receber_mesmoCallbackDuasVezes_delegaSempreAoServiceQueDeduplica() {
        prepararProcesso();

        escavadorCallbackService.receber(PAYLOAD, TOKEN, null);
        escavadorCallbackService.receber(PAYLOAD, TOKEN, null);

        verify(processoMovimentacaoService, times(2)).registrarMovimentacoes(eq(PROCESSO_ID), any());
    }

    @Test
    void receber_processoInexistente_gravaEventoComErroEmVezDeFalharOCallback() {
        when(processoRepository.findByNumeroCnjIgnoringTenant(anyString())).thenReturn(Optional.empty());

        escavadorCallbackService.receber(PAYLOAD, TOKEN, null);

        EscavadorCallbackEvento evento = ultimoEventoGravado();
        assertThat(evento.getProcessado()).isFalse();
        assertThat(evento.getErro()).contains("não cadastrado");
        assertThat(evento.getPayloadJson()).isEqualTo(PAYLOAD);
    }

    @Test
    void receber_payloadInvalido_gravaEventoComErroENaoPropaga() {
        escavadorCallbackService.receber("{ isso nao e json", TOKEN, null);

        EscavadorCallbackEvento evento = ultimoEventoGravado();
        assertThat(evento.getProcessado()).isFalse();
        assertThat(evento.getErro()).isNotBlank();
    }

    @Test
    void receber_eventoSemEfeitoNoCrm_marcaComoProcessadoSemTocarEmMovimentacoes() {
        String processoVerificado = """
                {
                    "event": "processo_verificado",
                    "monitoramento": { "id": 1912382, "numero": "%s", "status": "ENCONTRADO" },
                    "verificado_em": "2025-07-14T03:59:10+00:00"
                }
                """.formatted(NUMERO_CNJ);

        escavadorCallbackService.receber(processoVerificado, TOKEN, null);

        verify(processoMovimentacaoService, never()).registrarMovimentacoes(anyLong(), any());
        assertThat(ultimoEventoGravado().getProcessado()).isTrue();
    }

    private void prepararProcesso() {
        Processo processo = new Processo();
        processo.setId(PROCESSO_ID);
        processo.setPublicId("processo-1");
        processo.setEmpresaId(7L);
        processo.setNumeroCnj(NUMERO_CNJ);
        when(processoRepository.findByNumeroCnjIgnoringTenant(NUMERO_CNJ)).thenReturn(Optional.of(processo));
    }

    private EscavadorCallbackEvento ultimoEventoGravado() {
        ArgumentCaptor<EscavadorCallbackEvento> captor = ArgumentCaptor.forClass(EscavadorCallbackEvento.class);
        verify(escavadorCallbackEventoRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}

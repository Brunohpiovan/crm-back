package com.juridiqsystem.crm.service.escavador;

import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackMapper;
import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackProperties;
import com.juridiqsystem.crm.infra.escavador.EscavadorCallbackTokenValidator;
import com.juridiqsystem.crm.model.EscavadorCallbackEvento;
import com.juridiqsystem.crm.model.IntimacaoMonitoramento;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.ProcessoMonitoramento;
import com.juridiqsystem.crm.model.dtos.escavador.IntimacaoInput;
import com.juridiqsystem.crm.model.dtos.escavador.MovimentacaoInput;
import com.juridiqsystem.crm.model.enums.FonteMovimentacao;
import com.juridiqsystem.crm.repository.EscavadorCallbackEventoRepository;
import com.juridiqsystem.crm.repository.IntimacaoMonitoramentoRepository;
import com.juridiqsystem.crm.repository.ProcessoMonitoramentoRepository;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.service.IntimacaoService;
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

import java.util.ArrayList;
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
    private static final Long OUTRO_PROCESSO_ID = 20L;
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
    private ProcessoMonitoramentoRepository processoMonitoramentoRepository;

    @Mock
    private ProcessoMovimentacaoService processoMovimentacaoService;

    @Mock
    private ProcessoMonitoramentoService processoMonitoramentoService;

    @Mock
    private IntimacaoMonitoramentoRepository intimacaoMonitoramentoRepository;

    @Mock
    private IntimacaoService intimacaoService;

    @Captor
    private ArgumentCaptor<List<MovimentacaoInput>> movimentacoesCaptor;

    @Captor
    private ArgumentCaptor<IntimacaoInput> intimacaoInputCaptor;

    @InjectMocks
    private EscavadorCallbackService escavadorCallbackService;

    private final List<Processo> processosDoCnj = new ArrayList<>();

    @BeforeEach
    void configurar() {
        callbackProperties.setToken(TOKEN);
        processosDoCnj.clear();
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
        prepararProcessoMonitorado(PROCESSO_ID, 7L);

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
     * numero_cnj é único por empresa, não globalmente: duas empresas podem, de forma legítima,
     * acompanhar o mesmo processo público. Atender só a primeira faria a segunda pagar a cota de
     * monitoramento e nunca receber nada.
     */
    @Test
    void receber_mesmoCnjMonitoradoPorDuasEmpresas_registraNasDuas() {
        prepararProcessoMonitorado(PROCESSO_ID, 7L);
        prepararProcessoMonitorado(OUTRO_PROCESSO_ID, 9L);

        escavadorCallbackService.receber(PAYLOAD, TOKEN, null);

        verify(processoMovimentacaoService).registrarMovimentacoes(eq(PROCESSO_ID), any());
        verify(processoMovimentacaoService).registrarMovimentacoes(eq(OUTRO_PROCESSO_ID), any());
    }

    /** Quem só consultou o processo, sem monitoramento ativo, não recebe o acompanhamento. */
    @Test
    void receber_empresaSemMonitoramentoAtivo_naoRecebeMovimentacao() {
        prepararProcessoMonitorado(PROCESSO_ID, 7L);
        prepararProcessoSemMonitoramento(OUTRO_PROCESSO_ID, 9L);

        escavadorCallbackService.receber(PAYLOAD, TOKEN, null);

        verify(processoMovimentacaoService).registrarMovimentacoes(eq(PROCESSO_ID), any());
        verify(processoMovimentacaoService, never()).registrarMovimentacoes(eq(OUTRO_PROCESSO_ID), any());
    }

    /**
     * O reenvio automático da Escavador (até 11 tentativas) faz o mesmo payload chegar repetido.
     * A deduplicação é responsabilidade de registrarMovimentacoes; o que se garante aqui é que o
     * service delega sempre a ele e nunca abre um caminho de escrita próprio.
     */
    @Test
    void receber_mesmoCallbackDuasVezes_delegaSempreAoServiceQueDeduplica() {
        prepararProcessoMonitorado(PROCESSO_ID, 7L);

        escavadorCallbackService.receber(PAYLOAD, TOKEN, null);
        escavadorCallbackService.receber(PAYLOAD, TOKEN, null);

        verify(processoMovimentacaoService, times(2)).registrarMovimentacoes(eq(PROCESSO_ID), any());
    }

    /**
     * NAO_ENCONTRADO é terminal: a Escavador não monitora nem cobra. Manter ativo mostraria
     * "monitorando" para algo que ninguém acompanha e ainda ocuparia uma vaga da cota do plano.
     */
    @Test
    void receber_processoNaoEncontradoNoTribunal_desligaOMonitoramento() {
        ProcessoMonitoramento monitoramento = prepararProcessoMonitorado(PROCESSO_ID, 7L);
        String naoEncontrado = """
                {
                    "event": "processo_nao_encontrado",
                    "monitoramento": { "id": 1567024, "numero": "%s", "status": "NAO_ENCONTRADO" },
                    "uuid": "65b45990e91de83f8f40483102ce97ca"
                }
                """.formatted(NUMERO_CNJ);

        escavadorCallbackService.receber(naoEncontrado, TOKEN, null);

        verify(processoMonitoramentoService).desligarPorProcessoNaoEncontrado(monitoramento.getId());
        verify(processoMovimentacaoService, never()).registrarMovimentacoes(anyLong(), any());
        assertThat(ultimoEventoGravado().getProcessado()).isTrue();
    }

    @Test
    void receber_processoInexistente_gravaEventoComErroEmVezDeFalharOCallback() {
        when(processoRepository.findAllByNumeroCnjIgnoringTenant(anyString())).thenReturn(List.of());

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

    // --- diario_movimentacao_nova / diario_citacao_nova (monitoramento de OAB) ---

    private static final String DIARIO_MOVIMENTACAO_NOVA_PAYLOAD = """
            {
                "event": "diario_movimentacao_nova",
                "monitoramento": { "id": 555, "termo": "123456", "tipo": "TERMO" },
                "movimentacao": { "data": "2026-08-15", "tipo": "PUBLICACAO", "conteudo": "Intimação de sentença." },
                "processo": { "numero_cnj": "1002089-72.2023.8.26.0260" },
                "uuid": "abc123"
            }
            """;

    private static final String DIARIO_CITACAO_NOVA_PAYLOAD = """
            {
                "event": "diario_citacao_nova",
                "monitoramento": [
                    { "id": 555, "termo": "123456", "tipo": "TERMO" },
                    { "id": 777, "termo": "654321", "tipo": "TERMO" }
                ],
                "diario": { "id": 42, "nome": "Diário de Justiça de SP", "sigla": "DJSP", "data_publicacao": "2026-08-16", "link": "https://example.com/diario/42" },
                "pagina_diario": { "pagina": 7, "conteudo": "<p>Publicação sem processo identificado.</p>" },
                "uuid": "def456"
            }
            """;

    @Test
    void receber_diarioMovimentacaoNova_resolveTenantPeloIdDaAssinaturaERegistraNoIntimacaoService() {
        IntimacaoMonitoramento monitoramento = prepararIntimacaoMonitorada("555", 7L);

        escavadorCallbackService.receber(DIARIO_MOVIMENTACAO_NOVA_PAYLOAD, "Bearer " + TOKEN, null);

        verify(intimacaoService).registrarDoCallback(eq(monitoramento), intimacaoInputCaptor.capture());
        assertThat(intimacaoInputCaptor.getValue().numeroCnjIdentificado()).isEqualTo("1002089-72.2023.8.26.0260");
        assertThat(intimacaoInputCaptor.getValue().conteudo()).isEqualTo("Intimação de sentença.");

        EscavadorCallbackEvento evento = ultimoEventoGravado();
        assertThat(evento.getProcessado()).isTrue();
        assertThat(evento.getErro()).isNull();
    }

    /**
     * O caso mais importante de não ser descartado: a Escavador não identificou o processo, mas a
     * publicação ainda precisa aparecer na lista de intimações para atenção humana. Também cobre
     * "monitoramento" como array citando duas OABs nossas na mesma publicação — as duas devem ser
     * atendidas.
     */
    @Test
    void receber_diarioCitacaoNova_semProcessoIdentificado_naoDescartaERegistraParaCadaOabCitada() {
        IntimacaoMonitoramento monitoramentoUm = prepararIntimacaoMonitorada("555", 7L);
        IntimacaoMonitoramento monitoramentoDois = prepararIntimacaoMonitorada("777", 9L);

        escavadorCallbackService.receber(DIARIO_CITACAO_NOVA_PAYLOAD, TOKEN, null);

        verify(intimacaoService).registrarDoCallback(eq(monitoramentoUm), any());
        verify(intimacaoService).registrarDoCallback(eq(monitoramentoDois), any());
        assertThat(ultimoEventoGravado().getProcessado()).isTrue();
    }

    @Test
    void receber_diarioSemAssinaturaCorrespondente_naoChamaIntimacaoServiceMasMarcaProcessado() {
        when(intimacaoMonitoramentoRepository.findByEscavadorMonitoramentoIdIgnoringTenant(anyString()))
                .thenReturn(Optional.empty());

        escavadorCallbackService.receber(DIARIO_MOVIMENTACAO_NOVA_PAYLOAD, TOKEN, null);

        verify(intimacaoService, never()).registrarDoCallback(any(), any());
        assertThat(ultimoEventoGravado().getProcessado()).isTrue();
    }

    /** Reentrega do mesmo callback (até 11x): o service delega sempre, dedupe é responsabilidade do IntimacaoService. */
    @Test
    void receber_mesmoCallbackDeDiarioDuasVezes_delegaSempreAoServiceQueDeduplica() {
        prepararIntimacaoMonitorada("555", 7L);

        escavadorCallbackService.receber(DIARIO_MOVIMENTACAO_NOVA_PAYLOAD, TOKEN, null);
        escavadorCallbackService.receber(DIARIO_MOVIMENTACAO_NOVA_PAYLOAD, TOKEN, null);

        verify(intimacaoService, times(2)).registrarDoCallback(any(), any());
    }

    private IntimacaoMonitoramento prepararIntimacaoMonitorada(String escavadorMonitoramentoId, Long empresaId) {
        IntimacaoMonitoramento monitoramento = new IntimacaoMonitoramento();
        monitoramento.setId(Long.valueOf(escavadorMonitoramentoId));
        monitoramento.setEmpresaId(empresaId);
        monitoramento.setEscavadorMonitoramentoId(escavadorMonitoramentoId);
        monitoramento.setAtivo(true);
        when(intimacaoMonitoramentoRepository.findByEscavadorMonitoramentoIdIgnoringTenant(escavadorMonitoramentoId))
                .thenReturn(Optional.of(monitoramento));
        return monitoramento;
    }

    private ProcessoMonitoramento prepararProcessoMonitorado(Long processoId, Long empresaId) {
        Processo processo = registrarProcesso(processoId, empresaId);
        ProcessoMonitoramento monitoramento = new ProcessoMonitoramento();
        monitoramento.setId(processoId * 100);
        monitoramento.setProcesso(processo);
        monitoramento.setAtivo(true);
        when(processoMonitoramentoRepository.findByProcessoId(processoId)).thenReturn(Optional.of(monitoramento));
        return monitoramento;
    }

    private void prepararProcessoSemMonitoramento(Long processoId, Long empresaId) {
        registrarProcesso(processoId, empresaId);
        when(processoMonitoramentoRepository.findByProcessoId(processoId)).thenReturn(Optional.empty());
    }

    private Processo registrarProcesso(Long processoId, Long empresaId) {
        Processo processo = new Processo();
        processo.setId(processoId);
        processo.setPublicId("processo-" + processoId);
        processo.setEmpresaId(empresaId);
        processo.setNumeroCnj(NUMERO_CNJ);
        processosDoCnj.add(processo);
        when(processoRepository.findAllByNumeroCnjIgnoringTenant(NUMERO_CNJ)).thenReturn(List.copyOf(processosDoCnj));
        return processo;
    }

    private EscavadorCallbackEvento ultimoEventoGravado() {
        ArgumentCaptor<EscavadorCallbackEvento> captor = ArgumentCaptor.forClass(EscavadorCallbackEvento.class);
        verify(escavadorCallbackEventoRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}

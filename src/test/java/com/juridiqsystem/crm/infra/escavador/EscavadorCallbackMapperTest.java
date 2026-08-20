package com.juridiqsystem.crm.infra.escavador;

import com.juridiqsystem.crm.infra.escavador.dto.EscavadorCallbackDiarioPayload;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorCallbackPayload;
import com.juridiqsystem.crm.model.dtos.escavador.IntimacaoInput;
import com.juridiqsystem.crm.model.enums.FonteMovimentacao;
import com.juridiqsystem.crm.model.dtos.escavador.MovimentacaoInput;
import com.juridiqsystem.crm.service.exceptions.EscavadorApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Payloads copiados da documentação oficial (https://api.escavador.com/v2/docs/callbacks,
 * seção "Detalhes dos Callbacks"). Se a Escavador mudar o formato, é aqui que quebra primeiro —
 * de propósito, em vez de silenciosamente parar de registrar movimentações em produção.
 */
class EscavadorCallbackMapperTest {

    private static final String NOVA_MOVIMENTACAO_JSON = """
            {
                "event": "nova_movimentacao",
                "monitoramento": {
                    "id": 1567024,
                    "numero": "1002089-72.2023.8.26.0260",
                    "criado_em": "2024-10-02T18:01:34+00:00",
                    "data_ultima_verificacao": null,
                    "tribunais": [
                        { "id": 102, "nome": "Tribunal de Justiça de São Paulo", "sigla": "TJSP", "categoria": null }
                    ],
                    "frequencia": "DIARIA",
                    "status": "ENCONTRADO"
                },
                "movimentacao": {
                    "id": 23895909833,
                    "data": "2024-10-01",
                    "tipo": "ANDAMENTO",
                    "tipo_publicacao": null,
                    "classificacao_predita": {
                        "nome": "Antecipação de tutela",
                        "descricao": "É a decisão que concede o pedido de tutela antecipada.",
                        "hierarquia": "Movimentações do Magistrado > Decisão > Concessão"
                    },
                    "conteudo": "Pedido de Liminar/Antecipação de Tutela",
                    "texto_categoria": null,
                    "fonte": {
                        "processo_fonte_id": 538793371,
                        "fonte_id": 1,
                        "nome": "Tribunal de Justiça de São Paulo",
                        "tipo": "TRIBUNAL",
                        "sigla": "TJSP",
                        "grau": 1,
                        "grau_formatado": "Primeiro Grau"
                    }
                },
                "uuid": "65b45990e91de83f8f40483102ce97ca"
            }
            """;

    private static final String PROCESSO_VERIFICADO_JSON = """
            {
                "event": "processo_verificado",
                "monitoramento": {
                    "id": 1912382,
                    "numero": "2171513-81.2024.8.26.0000",
                    "frequencia": "DIARIA",
                    "status": "ENCONTRADO"
                },
                "verificado_em": "2025-07-14T03:59:10+00:00",
                "uuid": "36ebf954077e5b3f64a437a165f42a34"
            }
            """;

    /**
     * "monitoramento" como OBJETO ÚNICO (não array) — o formato-base descrito na documentação
     * para os eventos de processo; para os dois eventos de diário abaixo, a documentação local
     * marca o campo como podendo vir tanto como objeto único quanto como array (§9.13), por isso
     * os dois formatos são cobertos aqui.
     */
    private static final String DIARIO_MOVIMENTACAO_NOVA_OBJETO_UNICO_JSON = """
            {
                "event": "diario_movimentacao_nova",
                "monitoramento": { "id": 555, "termo": "123456", "tipo": "TERMO", "qtd_aparicoes": 3, "numero_diarios_monitorados": 12, "data_ultima_aparicao": "10/08/2026" },
                "movimentacao": { "data": "2026-08-15", "tipo": "PUBLICACAO", "conteudo": "Intimação de sentença." },
                "processo": { "numero_cnj": "1002089-72.2023.8.26.0260" },
                "envolvidos": [ { "nome": "Fulano de Tal" } ],
                "uuid": "abc123"
            }
            """;

    /** "monitoramento" como ARRAY — cobre o caso de a mesma publicação citar mais de uma OAB nossa. */
    private static final String DIARIO_CITACAO_NOVA_ARRAY_JSON = """
            {
                "event": "diario_citacao_nova",
                "monitoramento": [
                    { "id": 555, "termo": "123456", "tipo": "TERMO" },
                    { "id": 777, "termo": "654321", "tipo": "TERMO" }
                ],
                "diario": { "id": 42, "nome": "Diário de Justiça de SP", "sigla": "DJSP", "data_publicacao": "2026-08-16", "link": "https://example.com/diario/42" },
                "pagina_diario": { "pagina": 7, "conteudo": "<p>Publicação sem processo identificado.</p>", "link": "https://example.com/diario/42/pagina/7" },
                "uuid": "def456"
            }
            """;

    private final EscavadorCallbackMapper mapper = new EscavadorCallbackMapper();

    @Test
    void lerEvento_identificaEventosDeDiarioParaDecidirQualPayloadDesserializar() {
        assertThat(mapper.lerEvento(DIARIO_MOVIMENTACAO_NOVA_OBJETO_UNICO_JSON)).isEqualTo("diario_movimentacao_nova");
        assertThat(mapper.lerEvento(DIARIO_CITACAO_NOVA_ARRAY_JSON)).isEqualTo("diario_citacao_nova");
        assertThat(mapper.lerEvento(NOVA_MOVIMENTACAO_JSON)).isEqualTo("nova_movimentacao");

        assertThat(EscavadorCallbackDiarioPayload.isEventoDiario("diario_movimentacao_nova")).isTrue();
        assertThat(EscavadorCallbackDiarioPayload.isEventoDiario("diario_citacao_nova")).isTrue();
        assertThat(EscavadorCallbackDiarioPayload.isEventoDiario("nova_movimentacao")).isFalse();
    }

    @Test
    void parseDiario_monitoramentoComoObjetoUnico_toleraViaAcceptSingleValueAsArray() {
        EscavadorCallbackDiarioPayload payload = mapper.parseDiario(DIARIO_MOVIMENTACAO_NOVA_OBJETO_UNICO_JSON);

        assertThat(payload.monitoramento()).hasSize(1);
        assertThat(payload.monitoramento().get(0).id()).isEqualTo(555L);
        assertThat(payload.isCitacao()).isFalse();
        assertThat(payload.numeroCnjIdentificado()).isEqualTo("1002089-72.2023.8.26.0260");
        assertThat(payload.uuid()).isEqualTo("abc123");
    }

    @Test
    void parseDiario_monitoramentoComoArray_parseiaTodasAsReferencias() {
        EscavadorCallbackDiarioPayload payload = mapper.parseDiario(DIARIO_CITACAO_NOVA_ARRAY_JSON);

        assertThat(payload.monitoramento()).hasSize(2);
        assertThat(payload.monitoramento()).extracting(m -> m.id()).containsExactly(555L, 777L);
        assertThat(payload.isCitacao()).isTrue();
        // diario_citacao_nova nunca traz "processo": a Escavador não identificou o processo.
        assertThat(payload.numeroCnjIdentificado()).isNull();
    }

    @Test
    void toIntimacaoInput_diarioMovimentacaoNova_extraiConteudoDaMovimentacaoENumeroCnjIdentificado() {
        IntimacaoInput input = mapper.toIntimacaoInput(mapper.parseDiario(DIARIO_MOVIMENTACAO_NOVA_OBJETO_UNICO_JSON));

        assertThat(input.numeroCnjIdentificado()).isEqualTo("1002089-72.2023.8.26.0260");
        assertThat(input.conteudo()).isEqualTo("Intimação de sentença.");
        assertThat(input.uuidCallback()).isEqualTo("abc123");
        // Sem objeto "diario" neste payload (só chega em diario_citacao_nova, ver outro teste).
        assertThat(input.diarioId()).isNull();
        assertThat(input.pagina()).isNull();
    }

    /**
     * diario_citacao_nova é o caso mais importante de não ser descartado (Escavador não achou o
     * processo) — o input resultante precisa carregar diarioId/página para a chave de dedupe
     * (monitoramento_id+diario_id+página, ver IntimacaoService), mesmo sem processo identificado.
     */
    @Test
    void toIntimacaoInput_diarioCitacaoNova_extraiDiarioEPaginaSemNumeroCnj() {
        IntimacaoInput input = mapper.toIntimacaoInput(mapper.parseDiario(DIARIO_CITACAO_NOVA_ARRAY_JSON));

        assertThat(input.numeroCnjIdentificado()).isNull();
        assertThat(input.diarioNome()).isEqualTo("Diário de Justiça de SP");
        assertThat(input.diarioSigla()).isEqualTo("DJSP");
        assertThat(input.diarioData()).isEqualTo(LocalDate.of(2026, 8, 16));
        assertThat(input.diarioId()).isEqualTo("42");
        assertThat(input.pagina()).isEqualTo(7);
        assertThat(input.conteudo()).isEqualTo("<p>Publicação sem processo identificado.</p>");
        assertThat(input.link()).isEqualTo("https://example.com/diario/42");
        assertThat(input.uuidCallback()).isEqualTo("def456");
    }

    @Test
    void parseDiario_corpoInvalido_lancaEscavadorApiException() {
        assertThatThrownBy(() -> mapper.parseDiario("isso não é json"))
                .isInstanceOf(EscavadorApiException.class);
    }

    @Test
    void parse_callbackDeNovaMovimentacao_extraiEventoNumeroCnjEMovimentacao() {
        EscavadorCallbackPayload payload = mapper.parse(NOVA_MOVIMENTACAO_JSON);

        assertThat(payload.isNovaMovimentacao()).isTrue();
        assertThat(payload.numeroCnj()).isEqualTo("1002089-72.2023.8.26.0260");
        assertThat(payload.uuid()).isEqualTo("65b45990e91de83f8f40483102ce97ca");
        assertThat(payload.movimentacao().tipo()).isEqualTo("ANDAMENTO");
    }

    @Test
    void toMovimentacaoInputs_callbackDeNovaMovimentacao_marcaFonteComoCallbackDeMonitoramento() {
        List<MovimentacaoInput> inputs = mapper.toMovimentacaoInputs(mapper.parse(NOVA_MOVIMENTACAO_JSON));

        assertThat(inputs).hasSize(1);
        MovimentacaoInput input = inputs.get(0);
        assertThat(input.dataMovimentacao()).isEqualTo(LocalDateTime.of(2024, 10, 1, 0, 0));
        assertThat(input.tipo()).isEqualTo("ANDAMENTO");
        assertThat(input.conteudo()).isEqualTo("Pedido de Liminar/Antecipação de Tutela");
        assertThat(input.fonte()).isEqualTo(FonteMovimentacao.CALLBACK_MONITORAMENTO);
    }

    /** Campos novos do provedor não podem quebrar a desserialização (@JsonIgnoreProperties). */
    @Test
    void parse_eventoSemMovimentacao_naoEhNovaMovimentacaoENaoGeraInput() {
        EscavadorCallbackPayload payload = mapper.parse(PROCESSO_VERIFICADO_JSON);

        assertThat(payload.isNovaMovimentacao()).isFalse();
        assertThat(payload.numeroCnj()).isEqualTo("2171513-81.2024.8.26.0000");
        assertThat(mapper.toMovimentacaoInputs(payload)).isEmpty();
    }

    @Test
    void parse_corpoInvalido_lancaEscavadorApiException() {
        assertThatThrownBy(() -> mapper.parse("isso não é json"))
                .isInstanceOf(EscavadorApiException.class);
    }
}

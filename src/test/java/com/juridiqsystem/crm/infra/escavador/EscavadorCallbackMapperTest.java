package com.juridiqsystem.crm.infra.escavador;

import com.juridiqsystem.crm.infra.escavador.dto.EscavadorCallbackPayload;
import com.juridiqsystem.crm.model.dtos.escavador.FonteMovimentacao;
import com.juridiqsystem.crm.model.dtos.escavador.MovimentacaoInput;
import org.junit.jupiter.api.Test;

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

    private final EscavadorCallbackMapper mapper = new EscavadorCallbackMapper();

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

package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Corpo dos callbacks {@code diario_movimentacao_nova} e {@code diario_citacao_nova} (§9.13 da
 * documentação local — os únicos dois eventos v1 documentados com outline de campos, e mesmo assim
 * marcados como **não confirmados** em formato exato). Record separado de
 * {@link EscavadorCallbackPayload} (não reaproveitado, não alterado) porque o campo
 * {@code monitoramento} pode vir como objeto único ou como array nesses dois eventos — diferente
 * dos eventos de processo, que sempre trazem um objeto único.
 *
 * <p>{@code diario_movimentacao_nova} traz {@code movimentacao} + {@code processo} preenchidos (a
 * Escavador identificou o processo); {@code diario_citacao_nova} traz {@code diario} +
 * {@code pagina_diario} preenchidos (não identificou). Os dois grupos de campos são deixados juntos
 * neste único record — mais simples que duas subclasses, e todos os campos são opcionais de
 * qualquer forma (@JsonIgnoreProperties + ausência de @NotNull).</p>
 *
 * <p><b>Antes de confiar nesta lógica em produção</b>: assim que houver acesso à conta real da
 * Escavador, chame {@code POST /api/v1/monitoramentos/{id}/testcallback} para capturar um payload
 * real e ajustar os campos abaixo se divergirem.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorCallbackDiarioPayload(
        String event,

        /**
         * Tolera tanto um objeto único quanto um array — ver aviso na documentação local sobre o
         * formato desses dois eventos divergir do padrão de objeto único usado pelos eventos de
         * processo. ACCEPT_SINGLE_VALUE_AS_ARRAY evita precisar de um deserializer customizado.
         */
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        List<EscavadorMonitoramentoDiarioResponse> monitoramento,

        EscavadorCallbackDiarioMovimentacao movimentacao,
        EscavadorCallbackDiarioProcesso processo,
        EscavadorCallbackDiarioDiario diario,
        @JsonProperty("pagina_diario") EscavadorCallbackDiarioPagina paginaDiario,
        String uuid) {

    public static final String EVENTO_DIARIO_MOVIMENTACAO_NOVA = "diario_movimentacao_nova";
    public static final String EVENTO_DIARIO_CITACAO_NOVA = "diario_citacao_nova";

    public static boolean isEventoDiario(String event) {
        return EVENTO_DIARIO_MOVIMENTACAO_NOVA.equals(event) || EVENTO_DIARIO_CITACAO_NOVA.equals(event);
    }

    /**
     * diario_citacao_nova é o caso em que a Escavador NÃO conseguiu identificar o processo — é
     * justamente o caso mais importante de aparecer na lista de intimações (precisa de atenção
     * humana), então nunca é descartado.
     */
    public boolean isCitacao() {
        return EVENTO_DIARIO_CITACAO_NOVA.equals(event);
    }

    /** Só preenchido em diario_movimentacao_nova, quando a Escavador identifica o processo. */
    public String numeroCnjIdentificado() {
        return processo == null ? null : processo.numero();
    }
}

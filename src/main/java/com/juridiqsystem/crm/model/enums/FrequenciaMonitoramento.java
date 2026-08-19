package com.juridiqsystem.crm.model.enums;

/**
 * Cadência com que o robô do Escavador vai ao tribunal/diário oficial buscar novidades do
 * processo monitorado.
 *
 * <p>Só existem estes dois valores porque são os únicos aceitos pela API v2 do Escavador
 * (POST /api/v2/monitoramentos/processos, campo {@code frequencia} — ver
 * https://api.escavador.com/v2/docs/monitoramento-de-processos). Não existe frequência mensal
 * na API: oferecê-la na UI significaria enviar um valor que o provedor rejeita (ou troca
 * silenciosamente por DIARIA, gerando cobrança acima do esperado). O nome da constante é
 * exatamente o valor enviado no corpo da requisição — ver EscavadorMonitoramentoApi.</p>
 */
public enum FrequenciaMonitoramento {

    /** De segunda a sexta. Default do Escavador quando o campo é omitido. */
    DIARIA,

    /** Uma vez por semana, em dia escolhido pelo Escavador. */
    SEMANAL
}

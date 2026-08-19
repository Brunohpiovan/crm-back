package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Corpo dos callbacks da Escavador (POST na URL cadastrada no painel da API, content-type
 * application/json). Os sete eventos publicados compartilham a mesma casca
 * ({@code event}, {@code monitoramento}, {@code uuid}) e variam só no objeto específico —
 * mapeamos aqui apenas o de {@code nova_movimentacao}, o único que altera estado no CRM.
 *
 * <p>Eventos e seus nomes exatos (https://api.escavador.com/v2/docs/callbacks):
 * {@code novo_processo}, {@code atualizacao_processo_concluida}, {@code nova_movimentacao},
 * {@code novo_documento}, {@code processo_verificado}, {@code processo_encontrado},
 * {@code processo_nao_encontrado}.</p>
 *
 * @param uuid identificador do callback na Escavador; útil para rastrear reenvios (a Escavador
 *             tenta até 11 vezes quando a entrega falha, então o mesmo evento pode chegar
 *             repetido — a deduplicação em si é feita por
 *             ProcessoMovimentacaoService.registrarMovimentacoes).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorCallbackPayload(
        String event,
        EscavadorMonitoramentoResponse monitoramento,
        EscavadorCallbackMovimentacao movimentacao,
        String uuid) {

    public static final String EVENTO_NOVA_MOVIMENTACAO = "nova_movimentacao";

    public boolean isNovaMovimentacao() {
        return EVENTO_NOVA_MOVIMENTACAO.equals(event);
    }

    /** Numeração CNJ do processo monitorado, sempre presente em {@code monitoramento.numero}. */
    public String numeroCnj() {
        return monitoramento == null ? null : monitoramento.numero();
    }
}

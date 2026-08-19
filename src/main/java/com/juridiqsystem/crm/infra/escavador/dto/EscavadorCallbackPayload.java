package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Corpo dos callbacks da Escavador (POST na URL cadastrada no painel da API, content-type
 * application/json). Os sete eventos publicados compartilham a mesma casca
 * ({@code event}, {@code monitoramento}, {@code uuid}) e variam só no objeto específico —
 * mapeamos aqui os de {@code nova_movimentacao} e {@code novo_documento}, os únicos que alteram
 * estado no CRM.
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
        EscavadorCallbackDocumento documento,
        String uuid) {

    public static final String EVENTO_NOVA_MOVIMENTACAO = "nova_movimentacao";
    public static final String EVENTO_NOVO_DOCUMENTO = "novo_documento";
    public static final String EVENTO_PROCESSO_NAO_ENCONTRADO = "processo_nao_encontrado";

    public boolean isNovaMovimentacao() {
        return EVENTO_NOVA_MOVIMENTACAO.equals(event);
    }

    public boolean isNovoDocumento() {
        return EVENTO_NOVO_DOCUMENTO.equals(event);
    }

    /**
     * A Escavador foi ao tribunal e não localizou o processo: a assinatura não vira monitoramento
     * e não há cobrança. Estado terminal para aquela assinatura — não é uma falha transitória.
     */
    public boolean isProcessoNaoEncontrado() {
        return EVENTO_PROCESSO_NAO_ENCONTRADO.equals(event);
    }

    /** Numeração CNJ do processo monitorado, sempre presente em {@code monitoramento.numero}. */
    public String numeroCnj() {
        return monitoramento == null ? null : monitoramento.numero();
    }
}

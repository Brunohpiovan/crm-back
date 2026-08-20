package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Um item de GET /api/v1/monitoramentos/{id}/aparicoes (§9.4 "Listar aparições") — grátis,
 * paginado. Usado pela sincronização manual/agendada (ver IntimacaoMonitoramentoService.
 * sincronizarAparicoes) para recuperar publicações que a Escavador já detectou mas cujo callback
 * {@code diario_movimentacao_nova}/{@code diario_citacao_nova} não chegou (webhook fora do ar,
 * URL pública indisponível etc.) — é a rede de segurança complementar ao callback em tempo real.
 *
 * <p>A doc local só confirma estes 4 campos para este endpoint especificamente (diferente de
 * outros endpoints da mesma API, mais ricos) — não mapeamos diário/link/CNJ aqui porque não há
 * confirmação de que existam nesta resposta; @JsonIgnoreProperties absorve qualquer campo extra
 * que a API realmente devolva sem quebrar, mas também não inventamos nomes de campo não
 * confirmados. Se a conta real devolver mais campos, vale revisitar.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorAparicao(
        Long id,
        @JsonProperty("monitoramento_id") Long monitoramentoId,
        @JsonProperty("data_publicacao") String dataPublicacao,
        String conteudo) {
}

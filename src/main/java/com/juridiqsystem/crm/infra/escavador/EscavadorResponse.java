package com.juridiqsystem.crm.infra.escavador;

/**
 * Envelope de toda chamada feita via EscavadorClient. custoCentavos vem do header
 * "Creditos-Utilizados" da resposta (ver docs/integrations/escavador-api.md §Balance/Credits) — é
 * o valor publicado em EscavadorRequisicaoRealizadaEvent para o ledger de créditos do Prompt 2.
 */
public record EscavadorResponse<T>(T corpo, int custoCentavos, int statusHttp) {
}

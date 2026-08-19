package com.juridiqsystem.crm.infra.escavador;

/**
 * Publicado pelo EscavadorClient a cada chamada à API da Escavador (sucesso ou erro de negócio com
 * corpo válido) — único jeito do módulo de monitoramento/créditos (Prompt 2) registrar consumo, via
 * @EventListener. Nunca pular a publicação em nenhum caminho que chame a Escavador.
 */
public record EscavadorRequisicaoRealizadaEvent(Long empresaId, String endpoint, Integer custoCentavos, boolean sucesso) {
}

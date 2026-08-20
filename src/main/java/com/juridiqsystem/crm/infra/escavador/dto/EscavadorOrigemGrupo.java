package com.juridiqsystem.crm.infra.escavador.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Um grupo (agrupado por estado) da resposta de GET /api/v1/origens (§9.5) — resposta é um array
 * bruto, sem o wrapper {@code {items, paginator}} usual da maioria dos outros endpoints, por isso
 * é tipificado direto como {@code EscavadorOrigemGrupo[]} em vez de via
 * EscavadorClient.get(...).corpo() genérico paginado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EscavadorOrigemGrupo(
        String nome,
        List<EscavadorOrigemDiario> diarios) {
}

package com.juridiqsystem.crm.infra.escavador.dto;

import java.util.List;

/** Envelope items/links/paginator para "GET /processos/numero_cnj/{numero}/movimentacoes". */
public record EscavadorMovimentacaoPaginado(List<EscavadorMovimentacao> items, EscavadorLinks links, EscavadorPaginator paginator) {
}

package com.juridiqsystem.crm.service.escavador;

import java.util.List;

/**
 * Publicado por ProcessoMovimentacaoService.registrarMovimentacoes ao persistir pelo menos uma
 * movimentação nova — único ponto de entrada de movimentações no sistema. O Prompt 2 escuta este
 * evento (@EventListener) para disparar notificação em tempo real, tanto para atualização manual
 * quanto para callback de monitoramento.
 */
public record NovasMovimentacoesDetectadasEvent(Long processoId, String processoPublicId, Long empresaId, List<Long> movimentacaoIds) {
}

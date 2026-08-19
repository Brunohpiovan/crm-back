package com.juridiqsystem.crm.service.escavador;

/**
 * Publicado por ProcessoDocumentoService.registrarDocumentos ao persistir pelo menos um documento
 * novo — vale tanto para busca manual quanto para callback de monitoramento, mesmo espírito de
 * NovasMovimentacoesDetectadasEvent. NovoDocumentoNotificacaoService escuta este evento para avisar
 * o frontend em tempo real, mesmo se o usuário não estiver na tela do processo.
 */
public record NovoDocumentoDetectadoEvent(Long processoId, String processoPublicId, Long empresaId, String tituloDocumento) {
}

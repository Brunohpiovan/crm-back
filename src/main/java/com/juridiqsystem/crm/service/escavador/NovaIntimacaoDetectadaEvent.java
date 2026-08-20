package com.juridiqsystem.crm.service.escavador;

/**
 * Publicado por IntimacaoService.registrarDoCallback ao persistir uma intimação nova — mirror
 * exato de NovoDocumentoDetectadoEvent. IntimacaoNotificacaoService escuta este evento para avisar
 * o frontend em tempo real, mesmo se o usuário não estiver na página de Intimações.
 */
public record NovaIntimacaoDetectadaEvent(Long intimacaoId, Long empresaId, String oabNumero, String diarioNome) {
}

package com.juridiqsystem.crm.service.escavador;

/**
 * Publicado quando um resumo por IA termina de ser gerado na Escavador (detectado pela chamada
 * síncrona do usuário ou pelo EscavadorResumoIaScheduler em background) — ResumoIaNotificacaoService
 * escuta este evento para avisar o frontend em tempo real, mesmo se o usuário não estiver mais na
 * tela do processo. Mesmo padrão de NovasMovimentacoesDetectadasEvent.
 */
public record ResumoIaConcluidoEvent(Long processoId, String processoPublicId, Long empresaId, String numeroCnj) {
}

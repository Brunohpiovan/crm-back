package com.juridiqsystem.crm.model.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Payload publicado em /topic/empresa/{empresaId}/resumo-ia — canal geral da empresa (não escopado
 * por processo, ao contrário de ProcessoMovimentacaoNotificacaoDTO), para que o usuário seja avisado
 * mesmo estando em outra tela do sistema quando pediu a geração.
 */
@Schema(description = "Aviso de que o resumo por IA de um processo terminou de ser gerado.")
public record ResumoIaNotificacaoDTO(String processoPublicId, String numeroCnj, LocalDateTime geradoEm) {
}

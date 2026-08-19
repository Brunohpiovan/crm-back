package com.juridiqsystem.crm.model.dtos;

import java.time.LocalDateTime;

/** status: PENDENTE (solicitado, ainda gerando) ou CONCLUIDO (resumoIa/geradoEm preenchidos). */
public record ProcessoResumoIaResponse(String status, String resumoIa, LocalDateTime geradoEm) {
}

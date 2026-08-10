package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.OportunidadeHistorico;
import com.juridiqsystem.crm.model.enums.TipoEventoOportunidade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Um evento do histórico de uma oportunidade, para exibição em ordem cronológica (mais recente primeiro).")
public record OportunidadeHistoricoDTO(
        String autor,
        TipoEventoOportunidade tipo,
        String descricao,
        LocalDateTime dataHora
) {
    public OportunidadeHistoricoDTO(OportunidadeHistorico historico) {
        this(historico.getAutorNome(), historico.getTipo(), historico.getDescricao(), historico.getCriadoEm());
    }
}

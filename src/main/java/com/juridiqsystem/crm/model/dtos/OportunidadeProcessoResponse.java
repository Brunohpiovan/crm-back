package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.OportunidadeProcesso;
import com.juridiqsystem.crm.model.enums.ProcessoSituacao;

import java.time.LocalDateTime;

public record OportunidadeProcessoResponse(
        String processoPublicId,
        String numeroCnj,
        String tribunal,
        ProcessoSituacao situacao,
        LocalDateTime vinculadoEm,
        String vinculadoPorNome
) {

    public OportunidadeProcessoResponse(OportunidadeProcesso vinculo) {
        this(
                vinculo.getProcesso().getPublicId(),
                vinculo.getProcesso().getNumeroCnj(),
                vinculo.getProcesso().getTribunal(),
                vinculo.getProcesso().getSituacao(),
                vinculo.getVinculadoEm(),
                vinculo.getVinculadoPor().getNome()
        );
    }
}

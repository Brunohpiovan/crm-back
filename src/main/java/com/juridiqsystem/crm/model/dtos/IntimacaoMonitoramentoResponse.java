package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.IntimacaoMonitoramento;
import com.juridiqsystem.crm.model.Usuario;
import com.juridiqsystem.crm.model.enums.Uf;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Estado de uma OAB monitorada, como exibido na lista de OABs cadastradas (Settings). Mirror de ProcessoMonitoramentoDetailResponse, mas uma linha por OAB em vez de um único estado por processo.")
public record IntimacaoMonitoramentoResponse(

        Long id,

        boolean ativo,

        String oabNumero,

        Uf oabUf,

        @Schema(description = "publicId do advogado vinculado a esta OAB. Null quando nenhum foi selecionado.")
        String usuarioAdvogadoId,

        @Schema(description = "Nome do advogado vinculado, para exibição direta na lista sem round-trip extra.")
        String usuarioAdvogadoNome,

        @Schema(description = "true quando a assinatura já foi confirmada pela Escavador. Enquanto false, a ativação está em retentativa pelo scheduler de reconciliação.")
        boolean confirmadoNaEscavador,

        LocalDateTime ativadoEm) {

    public static IntimacaoMonitoramentoResponse from(IntimacaoMonitoramento monitoramento) {
        Usuario advogado = monitoramento.getUsuarioAdvogado();
        return new IntimacaoMonitoramentoResponse(
                monitoramento.getId(),
                Boolean.TRUE.equals(monitoramento.getAtivo()),
                monitoramento.getOabNumero(),
                monitoramento.getOabUf(),
                advogado != null ? advogado.getPublicId() : null,
                advogado != null ? advogado.getNome() : null,
                monitoramento.getEscavadorMonitoramentoId() != null,
                monitoramento.getAtivadoEm());
    }
}

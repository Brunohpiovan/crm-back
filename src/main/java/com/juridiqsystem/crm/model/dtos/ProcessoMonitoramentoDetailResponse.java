package com.juridiqsystem.crm.model.dtos;

import com.juridiqsystem.crm.model.ProcessoMonitoramento;
import com.juridiqsystem.crm.model.enums.FrequenciaMonitoramento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Estado atual do monitoramento de um processo, como exibido no card de monitoramento.")
public record ProcessoMonitoramentoDetailResponse(

        @Schema(description = "false quando o processo nunca foi monitorado ou o monitoramento está desligado.")
        boolean ativo,

        FrequenciaMonitoramento frequencia,

        boolean comDocumentos,

        @Schema(description = "true quando a assinatura já foi confirmada pela Escavador. Enquanto false, a ativação está em retentativa pelo scheduler de reconciliação.")
        boolean confirmadoNaEscavador,

        LocalDateTime ativadoEm) {

    /** Estado exibido para um processo que nunca teve monitoramento (ou que foi desligado). */
    public static ProcessoMonitoramentoDetailResponse inativo() {
        return new ProcessoMonitoramentoDetailResponse(false, null, false, false, null);
    }

    public static ProcessoMonitoramentoDetailResponse from(ProcessoMonitoramento monitoramento) {
        if (monitoramento == null || !Boolean.TRUE.equals(monitoramento.getAtivo())) {
            return inativo();
        }
        return new ProcessoMonitoramentoDetailResponse(
                true,
                monitoramento.getFrequencia(),
                Boolean.TRUE.equals(monitoramento.getComDocumentos()),
                monitoramento.getEscavadorMonitoramentoId() != null,
                monitoramento.getAtivadoEm());
    }
}

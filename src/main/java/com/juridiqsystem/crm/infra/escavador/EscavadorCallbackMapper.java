package com.juridiqsystem.crm.infra.escavador;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorCallbackMovimentacao;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorCallbackPayload;
import com.juridiqsystem.crm.model.dtos.escavador.MovimentacaoInput;
import com.juridiqsystem.crm.model.enums.FonteMovimentacao;
import com.juridiqsystem.crm.service.exceptions.EscavadorApiException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Único lugar do sistema que entende o formato bruto do callback da Escavador — mesmo papel de
 * MetaWebhookMapper para os webhooks da Meta. A regra de negócio (EscavadorCallbackService) só
 * enxerga EscavadorCallbackPayload e MovimentacaoInput.
 */
@Component
public class EscavadorCallbackMapper {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public EscavadorCallbackPayload parse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, EscavadorCallbackPayload.class);
        } catch (Exception e) {
            throw new EscavadorApiException("Corpo do callback da Escavador não é um JSON válido.", e);
        }
    }

    /**
     * Converte a movimentação do callback no input consumido por
     * ProcessoMovimentacaoService.registrarMovimentacoes. Devolve lista (e não um único item)
     * porque é assim que o service recebe — e porque um callback sem movimentação (ex.: evento de
     * processo verificado) simplesmente não gera nenhum input.
     */
    public List<MovimentacaoInput> toMovimentacaoInputs(EscavadorCallbackPayload payload) {
        EscavadorCallbackMovimentacao movimentacao = payload.movimentacao();
        if (movimentacao == null || movimentacao.conteudo() == null || movimentacao.conteudo().isBlank()) {
            return List.of();
        }
        return List.of(new MovimentacaoInput(
                parseData(movimentacao.data()),
                movimentacao.tipo(),
                movimentacao.conteudo(),
                FonteMovimentacao.CALLBACK_MONITORAMENTO));
    }

    /**
     * O campo {@code data} do callback vem como "yyyy-MM-dd" (sem hora). Aceitamos também o
     * formato com hora para não quebrar caso a Escavador passe a enviá-lo, e caímos para o
     * instante da recepção se o valor vier ausente/ilegível — perder a movimentação inteira por
     * causa de um formato de data inesperado seria pior que registrá-la com a data de chegada.
     */
    private LocalDateTime parseData(String data) {
        if (data == null || data.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDate.parse(data.substring(0, 10)).atStartOfDay();
        } catch (DateTimeParseException | IndexOutOfBoundsException e) {
            return LocalDateTime.now();
        }
    }
}

package com.juridiqsystem.crm.infra.escavador;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorCallbackDiarioPayload;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorCallbackMovimentacao;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorCallbackPayload;
import com.juridiqsystem.crm.model.dtos.escavador.IntimacaoInput;
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
     * Primeira fase da leitura de um callback: só o campo {@code event}, sem desserializar o
     * resto do corpo. Necessário porque o formato de {@code monitoramento} diverge entre os
     * eventos de processo (objeto único, {@link EscavadorCallbackPayload}) e os de diário (objeto
     * ou array, {@link EscavadorCallbackDiarioPayload}) — EscavadorCallbackService usa o retorno
     * daqui para decidir qual dos dois desserializar de fato.
     */
    public String lerEvento(String rawBody) {
        try {
            JsonNode node = objectMapper.readTree(rawBody);
            JsonNode eventNode = node.get("event");
            return eventNode == null || eventNode.isNull() ? null : eventNode.asText();
        } catch (Exception e) {
            throw new EscavadorApiException("Corpo do callback da Escavador não é um JSON válido.", e);
        }
    }

    public EscavadorCallbackDiarioPayload parseDiario(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, EscavadorCallbackDiarioPayload.class);
        } catch (Exception e) {
            throw new EscavadorApiException("Corpo do callback da Escavador (diário) não é um JSON válido.", e);
        }
    }

    /**
     * Converte os dados da publicação encontrada no input consumido por
     * IntimacaoService.registrarDoCallback — mesmo papel de toMovimentacaoInputs. Vale tanto para
     * diario_movimentacao_nova (usa movimentacao.conteudo + processo, quando presentes) quanto
     * diario_citacao_nova (usa diario/pagina_diario, quando presentes); nenhum dos dois grupos de
     * campo é obrigatório aqui — o parser é defensivo por causa do formato não confirmado (ver
     * EscavadorCallbackDiarioPayload).
     */
    public IntimacaoInput toIntimacaoInput(EscavadorCallbackDiarioPayload payload) {
        String conteudo = payload.movimentacao() != null && payload.movimentacao().conteudo() != null
                ? payload.movimentacao().conteudo()
                : payload.paginaDiario() != null ? payload.paginaDiario().conteudo() : null;
        String link = payload.diario() != null && payload.diario().link() != null
                ? payload.diario().link()
                : payload.paginaDiario() != null ? payload.paginaDiario().link() : null;
        String diarioNome = payload.diario() != null ? payload.diario().nome() : null;
        String diarioSigla = payload.diario() != null ? payload.diario().sigla() : null;
        LocalDate diarioData = payload.diario() != null ? parseDataDiario(payload.diario().data()) : null;
        String diarioId = payload.diario() != null && payload.diario().id() != null
                ? String.valueOf(payload.diario().id())
                : null;
        Integer pagina = payload.paginaDiario() != null ? payload.paginaDiario().pagina() : null;

        return new IntimacaoInput(
                payload.numeroCnjIdentificado(), diarioNome, diarioSigla, diarioData, conteudo, link,
                diarioId, pagina, payload.uuid(), null);
    }

    private LocalDate parseDataDiario(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(data.trim().substring(0, Math.min(10, data.trim().length())));
        } catch (Exception e) {
            return null;
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

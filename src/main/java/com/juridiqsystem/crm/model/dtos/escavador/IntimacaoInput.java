package com.juridiqsystem.crm.model.dtos.escavador;

import java.time.LocalDate;

/**
 * Entrada de IntimacaoService.registrarDoCallback — contrato entre quem entende o formato bruto de
 * cada fonte (EscavadorCallbackMapper para os callbacks {@code diario_movimentacao_nova}/
 * {@code diario_citacao_nova}, ou IntimacaoMonitoramentoService.sincronizarAparicoes para a
 * sincronização manual/agendada via GET .../aparicoes) e o service (que não conhece o formato do
 * provedor). O service escolhe a chave de dedupe pela prioridade: {@code escavadorAparicaoId}
 * (estável, vindo da sincronização) > {@code diarioId}+{@code pagina} (vindo de callback) >
 * fallback por uuid — ver IntimacaoService.construirChaveDedupe. Só um desses grupos vem
 * preenchido por entrada, dependendo de quem construiu o input.
 *
 * @param escavadorAparicaoId id da aparição na Escavador (endpoint .../aparicoes) — chave de
 *                            dedupe estável para a sincronização; não vem preenchido em entradas
 *                            construídas a partir de callback.
 * @param diarioId            id do diário do callback; nulo/nem sempre presente (formato do
 *                            payload de callback não totalmente confirmado, ver
 *                            EscavadorCallbackDiarioPayload). Não preenchido pela sincronização.
 * @param pagina               página do diário do callback; mesma ressalva de diarioId.
 * @param uuidCallback         uuid do evento de callback, usado como fallback de dedupe quando
 *                             diarioId/pagina não vêm preenchidos. Não preenchido pela
 *                             sincronização (que já tem escavadorAparicaoId, mais estável).
 */
public record IntimacaoInput(
        String numeroCnjIdentificado,
        String diarioNome,
        String diarioSigla,
        LocalDate diarioData,
        String conteudo,
        String link,
        String diarioId,
        Integer pagina,
        String uuidCallback,
        Long escavadorAparicaoId) {
}

package com.juridiqsystem.crm.infra.escavador;

import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMonitoramentoCreateRequest;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMonitoramentoResponse;
import com.juridiqsystem.crm.model.enums.FrequenciaMonitoramento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Fachada tipada dos endpoints de monitoramento da Escavador (API v2). Toda chamada passa pelo
 * EscavadorClient — que já autentica, contabiliza custo e publica
 * EscavadorRequisicaoRealizadaEvent — então nenhuma regra de negócio precisa conhecer path,
 * versão da API ou formato snake_case do provedor.
 *
 * <p>Endpoints cobertos (https://api.escavador.com/v2/docs/monitoramento-de-processos):
 * <ul>
 *   <li>POST /api/v2/monitoramentos/processos — cria a assinatura de um processo por CNJ.</li>
 *   <li>GET /api/v2/monitoramentos/processos/{id} — consulta o status da assinatura.</li>
 *   <li>DELETE /api/v2/monitoramentos/processos/{id} — remove a assinatura.</li>
 * </ul>
 *
 * <p>O monitoramento de NOVOS processos (/api/v2/monitoramentos/novos-processos, por termo/OAB/
 * documento) não é exposto aqui de propósito: o produto monitora processos já vinculados a uma
 * oportunidade, e cada método sem uso seria uma porta aberta para gasto não previsto na conta
 * compartilhada. Adicionar quando existir um caso de uso real.</p>
 */
@Component
public class EscavadorMonitoramentoApi {

    private static final String PATH_MONITORAMENTO_PROCESSOS = "/api/v2/monitoramentos/processos";

    @Autowired
    private EscavadorClient escavadorClient;

    /**
     * Cria a assinatura de monitoramento do processo. A assinatura nasce com status PENDENTE e
     * passa a ENCONTRADO quando o robô localiza o processo no tribunal (ou NAO_ENCONTRADO, caso
     * em que não há cobrança) — a transição chega por callback.
     *
     * @param numeroCnj     numeração CNJ do processo.
     * @param frequencia    cadência de verificação.
     * @param comDocumentos também buscar documentos públicos do processo.
     */
    public EscavadorMonitoramentoResponse criar(String numeroCnj, FrequenciaMonitoramento frequencia, boolean comDocumentos) {
        EscavadorMonitoramentoCreateRequest corpo = new EscavadorMonitoramentoCreateRequest(
                numeroCnj, null, frequencia.name(), comDocumentos);
        return escavadorClient
                .post(EscavadorApiVersion.V2, PATH_MONITORAMENTO_PROCESSOS, corpo, EscavadorMonitoramentoResponse.class)
                .corpo();
    }

    public EscavadorMonitoramentoResponse buscar(String escavadorMonitoramentoId) {
        return escavadorClient
                .get(EscavadorApiVersion.V2, PATH_MONITORAMENTO_PROCESSOS + "/" + escavadorMonitoramentoId, null,
                        EscavadorMonitoramentoResponse.class)
                .corpo();
    }

    /** Remove a assinatura na Escavador. Responde 204 sem corpo. */
    public void remover(String escavadorMonitoramentoId) {
        escavadorClient.delete(EscavadorApiVersion.V2, PATH_MONITORAMENTO_PROCESSOS + "/" + escavadorMonitoramentoId, Void.class);
    }
}

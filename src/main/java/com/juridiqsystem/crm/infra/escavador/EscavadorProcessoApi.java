package com.juridiqsystem.crm.infra.escavador;

import com.juridiqsystem.crm.infra.escavador.dto.EscavadorAtualizacaoSolicitacao;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMovimentacaoPaginado;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorProcesso;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorProcessoPaginado;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorResumoIa;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorResumoIaSolicitacao;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Métodos tipados sobre os endpoints de Consulta de Processos / Atualização / Resumo IA (v2) — ver
 * docs/integrations/escavador-api.md §8.3/§8.4/§8.5. Nenhum método aqui monta URL/headers na mão,
 * tudo delega a EscavadorClient.
 */
@Component
public class EscavadorProcessoApi {

    private final EscavadorClient client;

    public EscavadorProcessoApi(EscavadorClient client) {
        this.client = client;
    }

    public EscavadorProcesso consultarPorCnj(String numeroCnj) {
        return client.get(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj, null, EscavadorProcesso.class).corpo();
    }

    public EscavadorMovimentacaoPaginado consultarMovimentacoes(String numeroCnj) {
        return client.get(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj + "/movimentacoes", null, EscavadorMovimentacaoPaginado.class).corpo();
    }

    public EscavadorProcessoPaginado buscarPorEnvolvido(String nomeOuCpfCnpj) {
        return client.get(EscavadorApiVersion.V2, "/processos/envolvido", Map.of("nome_ou_cpf_cnpj", nomeOuCpfCnpj), EscavadorProcessoPaginado.class).corpo();
    }

    public EscavadorProcessoPaginado buscarPorOab(String oab) {
        return client.get(EscavadorApiVersion.V2, "/processos/advogado/" + oab, null, EscavadorProcessoPaginado.class).corpo();
    }

    /** Dispara a atualização assíncrona no tribunal (§8.3) — o chamador deve reconsultar consultarPorCnj/consultarMovimentacoes em seguida. */
    public EscavadorAtualizacaoSolicitacao atualizarNoTribunal(String numeroCnj) {
        return client.post(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj + "/solicitar-atualizacao", null, EscavadorAtualizacaoSolicitacao.class).corpo();
    }

    public EscavadorResumoIaSolicitacao solicitarResumoIa(String numeroCnj) {
        return client.post(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj + "/ia/resumo/solicitar-atualizacao", null, EscavadorResumoIaSolicitacao.class).corpo();
    }

    public EscavadorResumoIaSolicitacao statusResumoIa(String numeroCnj) {
        return client.get(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj + "/ia/resumo/status", null, EscavadorResumoIaSolicitacao.class).corpo();
    }

    /** Síncrono — 404 (mapeado para EscavadorApiException por EscavadorClient) se nenhum resumo foi gerado ainda. */
    public EscavadorResumoIa obterResumoIa(String numeroCnj) {
        return client.get(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj + "/ia/resumo", null, EscavadorResumoIa.class).corpo();
    }
}

package com.juridiqsystem.crm.infra.escavador;

import com.juridiqsystem.crm.config.CacheConfig;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorAparicao;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorAparicaoPaginado;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMonitoramentoDiarioCreateRequest;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMonitoramentoDiarioEnvelope;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMonitoramentoDiarioResponse;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorOrigemGrupo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Fachada tipada dos endpoints de monitoramento de Diários Oficiais da Escavador (API v1, §9.4/
 * §9.5). Mesmo papel de EscavadorMonitoramentoApi (que cobre a API v2 de monitoramento de
 * processos): toda chamada passa por EscavadorClient com {@link EscavadorApiVersion#V1}, sem
 * infra nova — ver EscavadorClient.
 */
@Component
public class EscavadorMonitoramentoDiarioApi {

    private static final String PATH_MONITORAMENTOS = "/monitoramentos";
    private static final String PATH_ORIGENS = "/origens";

    /**
     * Teto de páginas por chamada de {@link #listarAparicoes} — mesmo racional de
     * IntimacaoMonitoramentoScheduler.LIMITE_POR_EXECUCAO: uma OAB com um volume anormal de
     * aparições não pode transformar uma sincronização em um loop de centenas de chamadas. 50
     * páginas * 20 itens = até 1000 aparições por sincronização, mais que suficiente para o caso
     * de uso (recuperar o que um webhook fora do ar deixou de entregar).
     */
    private static final int LIMITE_PAGINAS_APARICOES = 50;

    private final EscavadorClient client;

    public EscavadorMonitoramentoDiarioApi(EscavadorClient client) {
        this.client = client;
    }

    /**
     * Cria a assinatura de monitoramento (tipo=TERMO, termo = número da OAB). Endpoint pago —
     * quem chama decide o momento e a cota (ver IntimacaoMonitoramentoService).
     */
    public EscavadorMonitoramentoDiarioResponse criar(String termo, List<Integer> origemIds) {
        EscavadorMonitoramentoDiarioCreateRequest corpo = EscavadorMonitoramentoDiarioCreateRequest.termo(termo, origemIds);
        EscavadorMonitoramentoDiarioEnvelope envelope = client
                .post(EscavadorApiVersion.V1, PATH_MONITORAMENTOS, corpo, EscavadorMonitoramentoDiarioEnvelope.class)
                .corpo();
        return envelope == null ? null : envelope.monitoramento();
    }

    /** Remove a assinatura na Escavador. Responde 204 sem corpo. */
    public void remover(String escavadorMonitoramentoId) {
        client.delete(EscavadorApiVersion.V1, PATH_MONITORAMENTOS + "/" + escavadorMonitoramentoId, Void.class);
    }

    /**
     * Lista todos os diários oficiais disponíveis, agrupados por estado — grátis. Cacheado em
     * memória (TTL de 12h, ver CacheConfig.ESCAVADOR_ORIGENS_CACHE): a lista praticamente não muda
     * e é consultada a cada ativação de OAB monitorada (resolução automática de origem_ids).
     *
     * <p>Resposta é um array bruto (sem o wrapper {items, paginator} usual), então tipificamos
     * direto como EscavadorOrigemGrupo[] em vez de usar o Class<T> genérico paginado.</p>
     */
    @Cacheable(CacheConfig.ESCAVADOR_ORIGENS_CACHE)
    public List<EscavadorOrigemGrupo> listarOrigens() {
        EscavadorOrigemGrupo[] resposta = client
                .get(EscavadorApiVersion.V1, PATH_ORIGENS, null, EscavadorOrigemGrupo[].class)
                .corpo();
        return resposta == null ? List.of() : Arrays.asList(resposta);
    }

    /**
     * Lista todas as aparições (publicações já encontradas) de uma assinatura — grátis. Rede de
     * segurança para quando o callback de monitoramento falha ou não chega (webhook fora do ar,
     * URL pública indisponível): a Escavador continua registrando as aparições do lado dela mesmo
     * sem entrega bem-sucedida, então dá pra recuperar consultando aqui. Percorre todas as páginas
     * até {@link #LIMITE_PAGINAS_APARICOES} — ver IntimacaoMonitoramentoService.sincronizarAparicoes.
     */
    public List<EscavadorAparicao> listarAparicoes(String escavadorMonitoramentoId) {
        List<EscavadorAparicao> todas = new ArrayList<>();
        String path = PATH_MONITORAMENTOS + "/" + escavadorMonitoramentoId + "/aparicoes";
        for (int pagina = 1; pagina <= LIMITE_PAGINAS_APARICOES; pagina++) {
            EscavadorAparicaoPaginado resposta = client
                    .get(EscavadorApiVersion.V1, path, Map.of("page", String.valueOf(pagina)), EscavadorAparicaoPaginado.class)
                    .corpo();
            if (resposta == null || resposta.items() == null || resposta.items().isEmpty()) {
                break;
            }
            todas.addAll(resposta.items());
            Integer totalPaginas = resposta.paginator() == null ? null : resposta.paginator().totalPages();
            if (totalPaginas == null || pagina >= totalPaginas) {
                break;
            }
        }
        return todas;
    }
}

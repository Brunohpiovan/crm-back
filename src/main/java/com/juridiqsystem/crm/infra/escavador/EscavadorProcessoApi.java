package com.juridiqsystem.crm.infra.escavador;

import com.juridiqsystem.crm.infra.escavador.dto.EscavadorAssunto;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorAtualizacaoSolicitacao;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorDocumento;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorDocumentoLinks;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorDocumentoPaginado;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorEnvolvido;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorEstado;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorLinks;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMovimentacao;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMovimentacaoFonte;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMovimentacaoPaginado;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorOab;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorPaginator;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorProcesso;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorProcessoFonte;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorProcessoFonteCapa;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorProcessoPaginado;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorResumoIa;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorResumoIaSolicitacao;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorTribunal;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorValorCausa;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Métodos tipados sobre os endpoints de Consulta de Processos / Atualização / Resumo IA (v2) — ver
 * docs/integrations/escavador-api.md §8.3/§8.4/§8.5. Nenhum método aqui monta URL/headers na mão,
 * tudo delega a EscavadorClient — exceto em modo mock (escavador.api.mock-mode=true, só dev local),
 * onde devolve dados fictícios fixos sem gastar saldo pago da conta Escavador. Ver
 * EscavadorApiProperties.mockMode.
 */
@Component
public class EscavadorProcessoApi {

    private static final String NUMERO_CNJ_MOCK_PADRAO = "0000001-23.2025.8.26.0100";
    /** Placeholder de PDF em modo mock — evita gastar o download (também pago) testando a UI. Não é um PDF válido de verdade. */
    private static final byte[] PDF_FICTICIO = "%PDF-1.4 documento ficticio (modo mock, sem custo)".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    private final EscavadorClient client;
    private final EscavadorApiProperties properties;

    public EscavadorProcessoApi(EscavadorClient client, EscavadorApiProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public EscavadorProcesso consultarPorCnj(String numeroCnj) {
        if (properties.isMockMode()) {
            return processoFicticio(numeroCnj);
        }
        return client.get(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj, null, EscavadorProcesso.class).corpo();
    }

    public EscavadorMovimentacaoPaginado consultarMovimentacoes(String numeroCnj) {
        if (properties.isMockMode()) {
            return movimentacoesFicticias();
        }
        return client.get(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj + "/movimentacoes", null, EscavadorMovimentacaoPaginado.class).corpo();
    }

    public EscavadorProcessoPaginado buscarPorEnvolvido(String nomeOuCpfCnpj) {
        if (properties.isMockMode()) {
            return buscaFicticia();
        }
        return client.get(EscavadorApiVersion.V2, "/processos/envolvido", Map.of("nome_ou_cpf_cnpj", nomeOuCpfCnpj), EscavadorProcessoPaginado.class).corpo();
    }

    public EscavadorProcessoPaginado buscarPorOab(String oab) {
        if (properties.isMockMode()) {
            return buscaFicticia();
        }
        return client.get(EscavadorApiVersion.V2, "/processos/advogado/" + oab, null, EscavadorProcessoPaginado.class).corpo();
    }

    /** Dispara a atualização assíncrona no tribunal (§8.3) — o chamador deve reconsultar consultarPorCnj/consultarMovimentacoes em seguida. */
    public EscavadorAtualizacaoSolicitacao atualizarNoTribunal(String numeroCnj) {
        if (properties.isMockMode()) {
            return new EscavadorAtualizacaoSolicitacao(1L, "SUCESSO", numeroCnj, "2026-08-19T12:00:00-03:00", "2026-08-19T12:00:05-03:00");
        }
        return client.post(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj + "/solicitar-atualizacao", null, EscavadorAtualizacaoSolicitacao.class).corpo();
    }

    public EscavadorResumoIaSolicitacao solicitarResumoIa(String numeroCnj) {
        if (properties.isMockMode()) {
            return resumoSolicitacaoFicticia(numeroCnj);
        }
        return client.post(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj + "/ia/resumo/solicitar-atualizacao", null, EscavadorResumoIaSolicitacao.class).corpo();
    }

    public EscavadorResumoIaSolicitacao statusResumoIa(String numeroCnj) {
        if (properties.isMockMode()) {
            // Sempre FINALIZADO: em modo mock a "geração" já está pronta no primeiro clique, sem
            // precisar simular o polling assíncrono real (§8.11) — conveniência para testar a UI.
            return resumoSolicitacaoFicticia(numeroCnj);
        }
        return client.get(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj + "/ia/resumo/status", null, EscavadorResumoIaSolicitacao.class).corpo();
    }

    /** Síncrono — 404 (mapeado para EscavadorApiException por EscavadorClient) se nenhum resumo foi gerado ainda. */
    public EscavadorResumoIa obterResumoIa(String numeroCnj) {
        if (properties.isMockMode()) {
            return resumoFicticio(numeroCnj);
        }
        return client.get(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj + "/ia/resumo", null, EscavadorResumoIa.class).corpo();
    }

    /** Chamada paga separada (§8.4) — não vem junto de consultarPorCnj. Só documentos públicos, nunca autos restritos. */
    public EscavadorDocumentoPaginado listarDocumentos(String numeroCnj) {
        if (properties.isMockMode()) {
            return documentosFicticios();
        }
        return client.get(EscavadorApiVersion.V2, "/processos/numero_cnj/" + numeroCnj + "/documentos", null, EscavadorDocumentoPaginado.class).corpo();
    }

    /** {@code documentoId} é o {@code key} do documento (ver EscavadorDocumento/EscavadorCallbackDocumento). Billed per download. */
    public byte[] baixarDocumentoPdf(String numeroCnj, String documentoId) {
        if (properties.isMockMode()) {
            return PDF_FICTICIO;
        }
        return client.getBinario(EscavadorApiVersion.V2,
                "/processos/numero_cnj/" + numeroCnj + "/documentos/" + documentoId + "/pdf").corpo();
    }

    // --- Dados fictícios de modo mock (escavador.api.mock-mode=true) ---

    private EscavadorResumoIaSolicitacao resumoSolicitacaoFicticia(String numeroCnj) {
        return new EscavadorResumoIaSolicitacao(1L, "FINALIZADO", "2026-08-19T12:00:00-03:00", numeroCnj, "2026-08-19T12:00:05-03:00", "NAO");
    }

    private EscavadorResumoIa resumoFicticio(String numeroCnj) {
        return new EscavadorResumoIa(
                numeroCnj,
                "Resumo gerado em modo mock (dev local, sem custo): processo cível de indenização por "
                        + "dano moral, atualmente na fase de sentença, com decisão de procedência parcial do pedido. "
                        + "Texto fictício — não reflete nenhum processo real.",
                "2026-08-19T12:00:05-03:00"
        );
    }

    private EscavadorProcessoPaginado buscaFicticia() {
        EscavadorProcesso processo = processoFicticio(NUMERO_CNJ_MOCK_PADRAO);
        return new EscavadorProcessoPaginado(List.of(processo), new EscavadorLinks(null, null, null, null), new EscavadorPaginator(1, 20, 1, 1));
    }

    private EscavadorMovimentacaoPaginado movimentacoesFicticias() {
        EscavadorMovimentacaoFonte fonte = new EscavadorMovimentacaoFonte(1L, "Tribunal de Justiça de São Paulo", "TRIBUNAL", "TJSP", 1, "Primeiro Grau");
        List<EscavadorMovimentacao> items = List.of(
                new EscavadorMovimentacao(1L, "2025-03-10", "PUBLICAÇÃO", "Distribuição do processo por sorteio (mock).", fonte),
                new EscavadorMovimentacao(2L, "2025-04-02", "ANDAMENTO", "Citação do réu realizada com sucesso (mock).", fonte),
                new EscavadorMovimentacao(3L, "2025-05-15", "ANDAMENTO", "Audiência de conciliação designada (mock).", fonte),
                new EscavadorMovimentacao(4L, "2025-06-20", "PUBLICAÇÃO", "Sentença publicada: pedido julgado parcialmente procedente (mock).", fonte)
        );
        return new EscavadorMovimentacaoPaginado(items, new EscavadorLinks(null, null, null, null), new EscavadorPaginator(1, 20, items.size(), 1));
    }

    private EscavadorDocumentoPaginado documentosFicticios() {
        List<EscavadorDocumento> items = List.of(
                new EscavadorDocumento(1L, "Petição Inicial", "Petição Inicial", "2025-03-10", "PUBLICO", "pdf", 5,
                        "mock-doc-key-1", new EscavadorDocumentoLinks(null), "2099-01-01T00:00:00-03:00"),
                new EscavadorDocumento(2L, "Sentença", "Sentença", "2025-06-20", "PUBLICO", "pdf", 12,
                        "mock-doc-key-2", new EscavadorDocumentoLinks(null), "2099-01-01T00:00:00-03:00")
        );
        return new EscavadorDocumentoPaginado(items, new EscavadorLinks(null, null, null, null), new EscavadorPaginator(1, 20, items.size(), 1));
    }

    private EscavadorProcesso processoFicticio(String numeroCnj) {
        EscavadorEstado estado = new EscavadorEstado("São Paulo", "SP");
        EscavadorValorCausa valorCausa = new EscavadorValorCausa("15000.00", "BRL", "R$ 15.000,00");
        EscavadorProcessoFonteCapa capa = new EscavadorProcessoFonteCapa(
                "Procedimento Comum Cível",
                "Indenização por Dano Moral",
                List.<EscavadorAssunto>of(),
                null,
                "Cível",
                "ATIVO",
                "2ª Vara Cível",
                null,
                valorCausa,
                "2025-03-10",
                null,
                List.of()
        );

        List<EscavadorOab> oabAdvogado = List.of(new EscavadorOab("123456", "SP", "ADVOGADO"));
        EscavadorEnvolvido advogado = new EscavadorEnvolvido(
                "Dr. Advogado Fictício", 1, "FISICA", null, null, "ADVOGADO", "ADVOGADO", "ADVOGADO",
                null, null, oabAdvogado, null);
        EscavadorEnvolvido autor = new EscavadorEnvolvido(
                "Fulano de Tal (mock)", 1, "FISICA", null, null, "AUTOR", "AUTOR", "ATIVO",
                "11122233344", null, null, List.of(advogado));
        EscavadorEnvolvido reu = new EscavadorEnvolvido(
                "Empresa Ré Ltda (mock)", 1, "JURIDICA", null, null, "REU", "REU", "PASSIVO",
                null, "11222333000144", null, null);

        EscavadorTribunal tribunal = new EscavadorTribunal(1L, "Tribunal de Justiça de São Paulo", "TJSP", null);
        EscavadorProcessoFonte fonte = new EscavadorProcessoFonte(
                1L, 1L, "Processo Cível", "Tribunal de Justiça de São Paulo", "TJSP", "TRIBUNAL",
                "2025-03-10", "2025-06-20", false, false, "ATIVO", 1, "Primeiro Grau", false, "PJE",
                null, 4, "2026-08-19", 3, List.of(autor, reu, advogado), capa, tribunal);

        return new EscavadorProcesso(
                numeroCnj, "Fulano de Tal (mock)", "Empresa Ré Ltda (mock)", 2025, "2025-03-10",
                estado, null, "2025-06-20", 4, "2026-08-19", List.of(fonte));
    }
}

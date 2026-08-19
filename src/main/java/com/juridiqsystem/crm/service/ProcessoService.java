package com.juridiqsystem.crm.service;

import com.juridiqsystem.crm.infra.escavador.EscavadorProcessoApi;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorEnvolvido;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorMovimentacao;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorProcesso;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorProcessoFonte;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorProcessoFonteCapa;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorProcessoPaginado;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorResumoIa;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorResumoIaSolicitacao;
import com.juridiqsystem.crm.infra.escavador.dto.EscavadorValorCausa;
import com.juridiqsystem.crm.infra.security.TenantContext;
import com.juridiqsystem.crm.model.Processo;
import com.juridiqsystem.crm.model.ProcessoEnvolvido;
import com.juridiqsystem.crm.model.ProcessoMovimentacao;
import com.juridiqsystem.crm.model.dtos.ProcessoBuscaResultResponse;
import com.juridiqsystem.crm.model.dtos.ProcessoDetailResponse;
import com.juridiqsystem.crm.model.dtos.ProcessoResumoIaResponse;
import com.juridiqsystem.crm.model.dtos.ProcessoSummaryResponse;
import com.juridiqsystem.crm.model.dtos.escavador.MovimentacaoInput;
import com.juridiqsystem.crm.model.enums.FonteMovimentacao;
import com.juridiqsystem.crm.model.enums.ProcessoSituacao;
import com.juridiqsystem.crm.model.enums.TipoEnvolvido;
import com.juridiqsystem.crm.repository.ProcessoEnvolvidoRepository;
import com.juridiqsystem.crm.repository.ProcessoMovimentacaoRepository;
import com.juridiqsystem.crm.repository.ProcessoRepository;
import com.juridiqsystem.crm.service.exceptions.EscavadorApiException;
import com.juridiqsystem.crm.service.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orquestra consulta na Escavador (via EscavadorProcessoApi) + upsert local de
 * Processo/ProcessoEnvolvido/ProcessoMovimentacao a partir da capa retornada. Chamadas à Escavador
 * (rede) rodam fora de transação — só o upsert em si é transacional, mesmo racional documentado em
 * OportunidadeComentarioService (a transação JPA nunca fica presa esperando uma chamada de rede).
 */
@Service
public class ProcessoService {

    private final EscavadorProcessoApi escavadorProcessoApi;
    private final ProcessoRepository processoRepository;
    private final ProcessoEnvolvidoRepository processoEnvolvidoRepository;
    private final ProcessoMovimentacaoRepository processoMovimentacaoRepository;
    private final ProcessoMovimentacaoService processoMovimentacaoService;
    /** Proxy autoinjetado (@Lazy quebra o ciclo de criação do bean) — chamadas internas passam por
     * "self" em vez de "this" para que @Transactional funcione (invocação direta dentro da mesma
     * classe não passa pelo proxy AOP do Spring e a anotação seria silenciosamente ignorada). */
    private final ProcessoService self;

    public ProcessoService(EscavadorProcessoApi escavadorProcessoApi,
                            ProcessoRepository processoRepository,
                            ProcessoEnvolvidoRepository processoEnvolvidoRepository,
                            ProcessoMovimentacaoRepository processoMovimentacaoRepository,
                            ProcessoMovimentacaoService processoMovimentacaoService,
                            @org.springframework.context.annotation.Lazy ProcessoService self) {
        this.escavadorProcessoApi = escavadorProcessoApi;
        this.processoRepository = processoRepository;
        this.processoEnvolvidoRepository = processoEnvolvidoRepository;
        this.processoMovimentacaoRepository = processoMovimentacaoRepository;
        this.processoMovimentacaoService = processoMovimentacaoService;
        this.self = self;
    }

    /** Consulta capa+envolvidos+movimentações na Escavador e faz upsert local. Não vincula a nenhuma Oportunidade. */
    public Processo consultarEUpsertPorCnj(String numeroCnj) {
        EscavadorProcesso remoto = escavadorProcessoApi.consultarPorCnj(numeroCnj);
        Processo processo = self.upsert(remoto);
        registrarMovimentacoesRemotas(processo, FonteMovimentacao.CONSULTA_INICIAL);
        return processo;
    }

    /** "Atualização do processo no tribunal" (§8.3): dispara o refresh e reconsolida a partir da releitura da capa/movimentações. */
    public Processo atualizarNoTribunal(String publicId) {
        Processo processo = buscarPorPublicIdOuFalhar(publicId);
        escavadorProcessoApi.atualizarNoTribunal(processo.getNumeroCnj());
        EscavadorProcesso remoto = escavadorProcessoApi.consultarPorCnj(processo.getNumeroCnj());
        Processo atualizado = self.upsert(remoto);
        registrarMovimentacoesRemotas(atualizado, FonteMovimentacao.ATUALIZACAO_MANUAL);
        return atualizado;
    }

    public List<ProcessoBuscaResultResponse> buscarPorEnvolvido(String documento, String nome) {
        String termo = (documento != null && !documento.isBlank()) ? documento.trim() : nome != null ? nome.trim() : null;
        if (termo == null || termo.isBlank()) {
            return List.of();
        }
        EscavadorProcessoPaginado resultado = escavadorProcessoApi.buscarPorEnvolvido(termo);
        return mapBusca(resultado);
    }

    public List<ProcessoBuscaResultResponse> buscarPorOab(String oab) {
        if (oab == null || oab.isBlank()) {
            return List.of();
        }
        EscavadorProcessoPaginado resultado = escavadorProcessoApi.buscarPorOab(oab.trim());
        return mapBusca(resultado);
    }

    public ProcessoDetailResponse buscarDetalheLocalPorPublicId(String publicId) {
        Processo processo = buscarPorPublicIdOuFalhar(publicId);
        return montarDetalhe(processo);
    }

    /** Base da tela /processos — só processos com pelo menos um vínculo em alguma Oportunidade. */
    public List<ProcessoSummaryResponse> listarVinculados(String search) {
        return processoRepository.findVinculadosComFiltro(search)
                .stream()
                .map(ProcessoSummaryResponse::new)
                .toList();
    }

    /**
     * Dispara/verifica a geração do resumo IA (fluxo assíncrono de 3 passos — §8.11) sem manter
     * uma requisição HTTP bloqueada em polling: primeiro tenta obter um status já concluído; se não
     * houver nenhuma solicitação em andamento, apenas dispara uma nova e devolve PENDENTE — o
     * frontend chama este mesmo endpoint de novo (ex.: reabrindo a tela) para checar se concluiu.
     */
    public ProcessoResumoIaResponse gerarOuObterResumoIa(String publicId) {
        Processo processo = buscarPorPublicIdOuFalhar(publicId);
        String numeroCnj = processo.getNumeroCnj();

        EscavadorResumoIaSolicitacao status = tentarObterStatusResumoIa(numeroCnj);
        if (status != null && "FINALIZADO".equalsIgnoreCase(status.status())) {
            EscavadorResumoIa resumo = escavadorProcessoApi.obterResumoIa(numeroCnj);
            return self.persistirResumo(processo, resumo);
        }
        if (status != null && "PENDENTE".equalsIgnoreCase(status.status())) {
            return new ProcessoResumoIaResponse("PENDENTE", null, null);
        }
        escavadorProcessoApi.solicitarResumoIa(numeroCnj);
        return new ProcessoResumoIaResponse("PENDENTE", null, null);
    }

    Processo buscarPorPublicIdOuFalhar(String publicId) {
        return processoRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo com id " + publicId + " nao encontrado"));
    }

    ProcessoDetailResponse montarDetalhe(Processo processo) {
        List<ProcessoEnvolvido> envolvidos = processoEnvolvidoRepository.findByProcessoIdOrderById(processo.getId());
        List<ProcessoMovimentacao> movimentacoes = processoMovimentacaoRepository.findByProcessoIdOrderByDataMovimentacaoDesc(processo.getId());
        return new ProcessoDetailResponse(processo, envolvidos, movimentacoes);
    }

    private EscavadorResumoIaSolicitacao tentarObterStatusResumoIa(String numeroCnj) {
        try {
            return escavadorProcessoApi.statusResumoIa(numeroCnj);
        } catch (EscavadorApiException e) {
            if (e.getStatusHttp() == 404) {
                return null;
            }
            throw e;
        }
    }

    @Transactional
    public ProcessoResumoIaResponse persistirResumo(Processo processo, EscavadorResumoIa resumo) {
        processo.setResumoIa(resumo.conteudo());
        processo.setResumoIaGeradoEm(LocalDateTime.now());
        processoRepository.save(processo);
        return new ProcessoResumoIaResponse("CONCLUIDO", resumo.conteudo(), processo.getResumoIaGeradoEm());
    }

    private void registrarMovimentacoesRemotas(Processo processo, FonteMovimentacao fonte) {
        var paginado = escavadorProcessoApi.consultarMovimentacoes(processo.getNumeroCnj());
        if (paginado == null || paginado.items() == null || paginado.items().isEmpty()) {
            return;
        }
        List<MovimentacaoInput> entradas = paginado.items().stream()
                .map(mov -> mapMovimentacaoInput(mov, fonte))
                .filter(input -> input != null)
                .toList();
        if (!entradas.isEmpty()) {
            processoMovimentacaoService.registrarMovimentacoes(processo.getId(), entradas);
        }
    }

    private MovimentacaoInput mapMovimentacaoInput(EscavadorMovimentacao movimentacao, FonteMovimentacao fonte) {
        LocalDate data = parseData(movimentacao.data());
        if (data == null || movimentacao.conteudo() == null) {
            return null;
        }
        return new MovimentacaoInput(data.atStartOfDay(), movimentacao.tipo(), movimentacao.conteudo(), fonte);
    }

    @Transactional
    public Processo upsert(EscavadorProcesso remoto) {
        Long empresaId = TenantContext.get();
        Processo processo = processoRepository.findByEmpresaIdAndNumeroCnj(empresaId, remoto.numeroCnj())
                .orElseGet(Processo::new);
        boolean novo = processo.getId() == null;
        if (novo) {
            processo.setEmpresaId(empresaId);
            processo.setNumeroCnj(remoto.numeroCnj());
            processo.setCriadoEm(LocalDateTime.now());
        }

        EscavadorProcessoFonte fontePrincipal = escolherFontePrincipal(remoto);
        EscavadorProcessoFonteCapa capa = fontePrincipal != null ? fontePrincipal.capa() : null;

        processo.setTribunal(fontePrincipal != null ? fontePrincipal.sigla() : null);
        processo.setInstancia(fontePrincipal != null ? fontePrincipal.grauFormatado() : null);
        processo.setSituacao(mapSituacao(capa != null ? capa.situacao() : null));
        processo.setValorCausa(mapValorCausa(capa != null ? capa.valorCausa() : null));
        processo.setDataDistribuicao(parseData(capa != null ? capa.dataDistribuicao() : null));
        processo.setArea(capa != null ? capa.area() : null);
        processo.setAssunto(capa != null ? capa.assunto() : null);
        processo.setUltimaConsultaEm(LocalDateTime.now());
        processo.setAtualizadoEm(LocalDateTime.now());

        Processo salvo = processoRepository.save(processo);

        processoEnvolvidoRepository.deleteAllByProcessoId(salvo.getId());
        List<EscavadorEnvolvido> envolvidosRemotos = fontePrincipal != null && fontePrincipal.envolvidos() != null
                ? fontePrincipal.envolvidos() : List.of();
        for (EscavadorEnvolvido envolvidoRemoto : envolvidosRemotos) {
            ProcessoEnvolvido envolvido = new ProcessoEnvolvido();
            envolvido.setProcesso(salvo);
            envolvido.setNome(envolvidoRemoto.nome());
            envolvido.setTipo(mapTipoEnvolvido(envolvidoRemoto.polo()));
            envolvido.setDocumento(envolvidoRemoto.cpf() != null ? envolvidoRemoto.cpf() : envolvidoRemoto.cnpj());
            envolvido.setOab(envolvidoRemoto.oabs() != null && !envolvidoRemoto.oabs().isEmpty()
                    ? envolvidoRemoto.oabs().get(0).numero() : null);
            processoEnvolvidoRepository.save(envolvido);
        }

        return salvo;
    }

    private EscavadorProcessoFonte escolherFontePrincipal(EscavadorProcesso remoto) {
        if (remoto.fontes() == null || remoto.fontes().isEmpty()) {
            return null;
        }
        return remoto.fontes().stream()
                .filter(f -> "TRIBUNAL".equalsIgnoreCase(f.tipo()))
                .findFirst()
                .orElse(remoto.fontes().get(0));
    }

    private List<ProcessoBuscaResultResponse> mapBusca(EscavadorProcessoPaginado resultado) {
        if (resultado == null || resultado.items() == null) {
            return List.of();
        }
        return resultado.items().stream().map(ProcessoBuscaResultResponse::new).toList();
    }

    private ProcessoSituacao mapSituacao(String situacao) {
        if (situacao == null) {
            return ProcessoSituacao.DESCONHECIDO;
        }
        try {
            return ProcessoSituacao.valueOf(situacao.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ProcessoSituacao.DESCONHECIDO;
        }
    }

    private TipoEnvolvido mapTipoEnvolvido(String polo) {
        if (polo == null) {
            return TipoEnvolvido.DESCONHECIDO;
        }
        return switch (polo.trim().toUpperCase()) {
            case "ADVOGADO" -> TipoEnvolvido.ADVOGADO;
            case "ATIVO" -> TipoEnvolvido.AUTOR;
            case "PASSIVO" -> TipoEnvolvido.REU;
            case "TERCEIRO" -> TipoEnvolvido.TERCEIRO;
            default -> TipoEnvolvido.DESCONHECIDO;
        };
    }

    private BigDecimal mapValorCausa(EscavadorValorCausa valorCausa) {
        if (valorCausa == null || valorCausa.valor() == null) {
            return null;
        }
        try {
            return new BigDecimal(valorCausa.valor().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(data.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
